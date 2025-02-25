package com.tune.picturebackend.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tune.picturebackend.api.aliyunai.AliYunAiApi;
import com.tune.picturebackend.api.aliyunai.model.CreateOutPaintingTaskRequest;
import com.tune.picturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.tune.picturebackend.common.DeleteRequest;
import com.tune.picturebackend.constant.UserConstant;
import com.tune.picturebackend.enums.PictureReviewStatusEnum;
import com.tune.picturebackend.exception.BusinessException;
import com.tune.picturebackend.exception.ErrorCode;
import com.tune.picturebackend.exception.ThrowUtils;
import com.tune.picturebackend.manager.CosManager;
import com.tune.picturebackend.manager.upload.FilePictureUpload;
import com.tune.picturebackend.manager.upload.PictureUploadTemplate;
import com.tune.picturebackend.manager.upload.UrlPictureUpload;
import com.tune.picturebackend.mapper.PictureMapper;
import com.tune.picturebackend.model.dto.file.UploadPictureResult;
import com.tune.picturebackend.model.dto.picture.*;
import com.tune.picturebackend.model.entity.Picture;
import com.tune.picturebackend.model.entity.Space;
import com.tune.picturebackend.model.entity.User;
import com.tune.picturebackend.model.vo.picture.LocalAvatarUploadVO;
import com.tune.picturebackend.model.vo.picture.PictureVO;
import com.tune.picturebackend.model.vo.user.UserVO;
import com.tune.picturebackend.service.PictureService;
import com.tune.picturebackend.service.SpaceService;
import com.tune.picturebackend.service.UserService;
import com.tune.picturebackend.utils.ColorSimilarUtils;
import com.tune.picturebackend.utils.ColorTransformUtils;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.awt.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Tune
 * @description 针对表【picture(图片)】的数据库操作Service实现
 */
@Slf4j
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureService {

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private UrlPictureUpload urlPictureUpload;

    @Resource
    private FilePictureUpload filePictureUpload;

    @Autowired
    private CosManager cosManager;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private TransactionTemplate transactionTemplate;

    private final Cache<String, String> LOCAL_CACHE =
            Caffeine.newBuilder().initialCapacity(1024)
                    .maximumSize(10000L)
                    // 缓存 5 分钟移除
                    .expireAfterWrite(5L, TimeUnit.MINUTES)
                    .build();

    @Resource
 	private AliYunAiApi aliYunAiApi;
    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest) {
        User loginUser = (User) StpUtil.getSession().get("loginUser");
        // 校验空间是否存在
        Long spaceId = pictureUploadRequest.getSpaceId();
        if (spaceId != null) {
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            // 校验额度
            if (space.getTotalCount() >= space.getMaxCount()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间条数不足");
            }
            if (space.getTotalSize() >= space.getMaxSize()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间大小不足");
            }
        }

        // 上传图片，得到信息
        // 按照用户 id 划分目录 => 按照空间划分目录
        String uploadPathPrefix;
        if (spaceId == null) {
            // 公共图库
            uploadPathPrefix = String.format("public/%s", loginUser.getId());
        } else {
            // 空间
            uploadPathPrefix = String.format("space/%s", spaceId);
        }

        // 根据 inputSource 的类型区分上传方式
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);

        // 若用户在同一窗口执行多次上传图片操作，在上传新图片时，删除在云端的对象存储刚刚上传的上一张旧图片
        Long oldPictureId = pictureUploadRequest.getId();
        if (oldPictureId != null) {
            try {
                Picture oldPicture = this.getById(oldPictureId);
                if (oldPicture != null) {
                    // 新图片上传成功，清除旧图片
                    clearPictureFile(oldPicture);
                    boolean removeResult = this.removeById(oldPictureId);
                    log.info("清理旧图片：{}，缩略图：{}", oldPicture.getUrl(), oldPicture.getThumbnailUrl());
                    log.info("清除数据库图片相关数据{}", removeResult ? "成功" : "失败");
                }
            } catch (Exception e) {
                log.error("上传的上一张图片id在数据库中无法查询到", e);
            }
        }

        // 构造要入库的图片信息
        Picture picture = new Picture();
        BeanUtil.copyProperties(uploadPictureResult, picture);
        // 支持外层传递图片名称
        if (StrUtil.isNotBlank(pictureUploadRequest.getPicName())) {
            String picName = pictureUploadRequest.getPicName();
            picture.setName(picName);
        }
        picture.setPicColor(ColorTransformUtils.getStandardColor(uploadPictureResult.getPicColor()));
        picture.setUserId(loginUser.getId());
        // 指定空间 id
        picture.setSpaceId(spaceId);
        // 补充审核参数
        fillReviewParams(picture);
        // 开启事务
        // 图片上传成功，减少相应空间额度
        transactionTemplate.execute(status -> {
            // 插入数据
            boolean result = this.saveOrUpdate(picture);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片数据上传数据库操作失败");
            if (spaceId != null) {
                // 更新空间的使用额度
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, spaceId)
                        .setSql("totalSize = totalSize + " + picture.getPicSize())
                        .setSql("totalCount = totalCount + 1")
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
            }
            return picture;
        });
        return PictureVO.objToVo(picture);
    }

    @Override
    public LocalAvatarUploadVO uploadAvatar(MultipartFile multipartFile) {
        User loginUser = (User) StpUtil.getSession().get("loginUser");
        String uploadPathPrefix = String.format("avatar/%s", loginUser.getId());
        // 头像上传方式只设置为文件上传
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(multipartFile, uploadPathPrefix);
        LocalAvatarUploadVO localAvatarUploadVO = new LocalAvatarUploadVO();
        System.out.println(uploadPictureResult.getUrl());
        localAvatarUploadVO.setAvatarUrl(uploadPictureResult.getUrl());
        return localAvatarUploadVO;
    }

    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        Long spaceId = pictureQueryRequest.getSpaceId();
        boolean nullSpaceId = pictureQueryRequest.isNullSpaceId();
        Date startEditTime = pictureQueryRequest.getStartEditTime();
        Date endEditTime = pictureQueryRequest.getEndEditTime();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        // 从多字段中搜索
        if (StrUtil.isNotBlank(searchText)) {
            // 需要拼接查询条件
            queryWrapper.and(qw -> qw.like("name", searchText)
                    .or()
                    .like("introduction", searchText)
            );
        }
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.isNull(nullSpaceId, "spaceId");
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);
        // >= startEditTime
        queryWrapper.ge(ObjUtil.isNotEmpty(startEditTime), "editTime", startEditTime);
        // < endEditTime
        queryWrapper.lt(ObjUtil.isNotEmpty(endEditTime), "editTime", endEditTime);
        // JSON 数组查询
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                // 对前端传递的 tag 数组(List<String>)每一项,通过增强 for 循环遍历和数据库的 tags 字段(String 类型的 JSON 数组)进行模糊查询
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public PictureVO getPictureVO(Picture picture) {
        // 对象转封装类
        PictureVO pictureVO = PictureVO.objToVo(picture);
        // 关联查询用户信息
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setUser(userVO);
        }
        return pictureVO;
    }

    /**
     * 分页获取图片封装
     */
    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage) {
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        // 对象列表 => 封装对象列表
        List<PictureVO> pictureVOList = pictureList.stream().map(PictureVO::objToVo).collect(Collectors.toList());
        // 1. 关联查询用户信息
        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 填充信息
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            pictureVO.setUser(userService.getUserVO(user));
        });
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }


    @Override
    public Page<PictureVO> getPictureVoPageWithCache(PictureQueryRequest pictureQueryRequest) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 普通用户默认只能查看已过审的数据
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        // 查询缓存
        String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest);
        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
        String cacheKey = String.format("picture:listPictureVOByPage:%s", hashKey);
        // 查询本地缓存
        String cachedValue = LOCAL_CACHE.getIfPresent(cacheKey);
        if (cachedValue!= null) {
            // 本地缓存命中，返回结果
            Page<PictureVO> cachedPage = JSONUtil.toBean(cachedValue, Page.class);
            log.info("本地缓存命中, cacheKey: {}", cacheKey);
            return cachedPage;
        }
        // 本地缓存未命中，查询 Redis 分布式缓存
        ValueOperations<String, String> opsForValue = stringRedisTemplate.opsForValue();
        cachedValue = opsForValue.get(cacheKey);
        if (cachedValue != null) {
            // 如果 Redis 缓存命中，更新本地缓存，返回结果
            LOCAL_CACHE.put(cacheKey, cachedValue);
            Page<PictureVO> cachedPage = JSONUtil.toBean(cachedValue, Page.class);
            log.info("redis 缓存命中, cacheKey: {}", cacheKey);
            return cachedPage;
        }

        // 3. 查询数据库
        Page<Picture> picturePage = this.page(new Page<>(current, size), this.getQueryWrapper(pictureQueryRequest));
        Page<PictureVO> pictureVOPage = this.getPictureVOPage(picturePage);
        log.info("缓存皆未命中，查询数据库");
        // 4. 更新缓存
        // 更新 Redis 缓存
        String cacheValue = JSONUtil.toJsonStr(pictureVOPage);
        // 设置缓存的过期时间，5 - 10 分钟过期，防止缓存雪崩
        int cacheExpireTime = 300 + RandomUtil.randomInt(0, 300);
        opsForValue.set(cacheKey, cacheValue, cacheExpireTime, TimeUnit.SECONDS);
        // 写入本地缓存
        LOCAL_CACHE.put(cacheKey, cacheValue);
        log.info("更新 Caffeine 缓存和 Redis 缓存");

        return pictureVOPage;
    }

    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        // 修改数据时，id 不能为空，有参数则校验
        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id 不能为空");
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url 过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }

    @Override
    public void deletePicture(DeleteRequest deleteRequest) {
        long pictureId = deleteRequest.getId();
        // 判断是否存在
        Picture oldPicture = this.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 开启事务
        // 图片删除成功，释放相应额度
        transactionTemplate.execute(status -> {
            // 操作数据库
            boolean result = this.removeById(pictureId);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
            if (oldPicture.getSpaceId() != null) {
                // 更新空间的使用额度
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, oldPicture.getSpaceId())
                        .setSql("totalSize = totalSize - " + oldPicture.getPicSize())
                        .setSql("totalCount = totalCount - 1")
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
            }
            return true;
        });
        // 异步清理图片文件
        // this.clearPictureFile(oldPicture);
    }

    @Override
    public boolean updatePicture(PictureUpdateRequest pictureUpdateRequest) {
        // 将实体类和 DTO 进行转换
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureUpdateRequest, picture);
        // 后端数据库 tags 字段存储的 String(JSON 数组),为方便前端使用,在获取的的时候转成了 List<String>
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));
        // 设置编辑时间
        picture.setEditTime(new Date());
        // 数据校验
        validPicture(picture);
        // 判断是否存在
        long id = pictureUpdateRequest.getId();
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 补充审核参数
        this.fillReviewParams(picture);
        // 操作数据库
        boolean result = this.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return true;
    }

    @Override
    public void editPicture(PictureEditRequest pictureEditRequest) {
        // 在此处将实体类和 DTO 进行转换
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest, picture);
        // 注意将 list 转为 string
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        // 设置编辑时间
        picture.setEditTime(new Date());
        // 数据校验
        this.validPicture(picture);
        // 判断是否存在
        long id = pictureEditRequest.getId();
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 补充审核参数
        this.fillReviewParams(picture);
        // 操作数据库
        boolean result = this.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest) {
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        PictureReviewStatusEnum reviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        if (id == null || reviewStatusEnum == null || PictureReviewStatusEnum.REVIEWING.equals(reviewStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 判断是否存在
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 已是该状态
        if (oldPicture.getReviewStatus().equals(reviewStatus)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请勿重复审核");
        }
        // 更新审核状态
        Picture updatePicture = new Picture();
        BeanUtils.copyProperties(pictureReviewRequest, updatePicture);

        User loginUser = (User) StpUtil.getSession().get("loginUser");
        updatePicture.setReviewerId(loginUser.getId());
        System.out.println("审核人ID" + loginUser.getId());
        updatePicture.setReviewTime(new Date());
        boolean result = this.updateById(updatePicture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public void fillReviewParams(Picture picture) {
        User loginUser = (User) StpUtil.getSession().get("loginUser");
        // 仅本人或管理员可编辑
        boolean isAdmin = StpUtil.hasRoleOr(UserConstant.ADMIN_ROLE, UserConstant.ROOT_ROLE);
        if (isAdmin) {
            // 管理员自动过审
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewerId(loginUser.getId());
            picture.setReviewMessage("自动过审");
            picture.setReviewTime(new Date());
        } else {
            // 非管理员，创建或编辑都要改为待审核
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }

    @Override
    public int uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest) {
        String searchText = pictureUploadByBatchRequest.getSearchText();
        String namePrefix = pictureUploadByBatchRequest.getNamePrefix();

        if (StrUtil.isBlank(namePrefix)) {
            namePrefix = searchText;
        }
        // 格式化数量
        Integer count = pictureUploadByBatchRequest.getCount();
        ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR, "最多 30 条");
        // 要抓取的地址
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        Document document;
        try {
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("获取页面失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取页面失败");
        }
        Element div = document.getElementsByClass("dgControl").first();
        if (ObjUtil.isNull(div)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取元素失败");
        }
        // Elements imgElementList = div.select("img.mimg");
        // 获取包含完整数据的元素
        Elements imgElementList = div.select(".iusc");

        int uploadCount = 0;
        for (Element imgElement : imgElementList) {
            // String fileUrl = imgElement.attr("src");
            // 获取data-m属性中的JSON字符串
            String dataM = imgElement.attr("m");
            String fileUrl;
            try {
                // 解析JSON字符串
                JSONObject jsonObject = JSONUtil.parseObj(dataM);
                // 获取murl字段（原始图片URL）
                fileUrl = jsonObject.getStr("murl");
            } catch (Exception e) {
                log.error("解析图片数据失败", e);
                continue;
            }

            if (StrUtil.isBlank(fileUrl)) {
                log.info("当前链接为空，已跳过: {}", fileUrl);
                continue;
            }
            // 处理图片上传地址，防止出现转义问题
            int questionMarkIndex = fileUrl.indexOf("?");
            if (questionMarkIndex > -1) {
                fileUrl = fileUrl.substring(0, questionMarkIndex);
            }
            // 上传图片
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            if (StrUtil.isNotBlank(namePrefix)) {
                // 设置图片名称，序号连续递增
                pictureUploadRequest.setPicName(namePrefix + (uploadCount + 1));
            }
            System.out.println("上传图片原地址：" + fileUrl);
            System.out.println("上传图片新名称：" + pictureUploadRequest.getPicName());
            try {
                PictureVO pictureVO = this.uploadPicture(fileUrl, pictureUploadRequest);
                log.info("图片上传成功, id = {}", pictureVO.getId());
                uploadCount++;
            } catch (Exception e) {
                log.error("图片上传失败", e);
                continue;
            }

            if (uploadCount >= count) {
                System.out.println("已全部上传，共 " + uploadCount + " 张图片");
                break;
            }
        }
        return uploadCount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest) {
        User loginUser = (User) StpUtil.getSession().get("loginUser");

        List<Long> pictureIdList = pictureEditByBatchRequest.getPictureIdList();
        Long spaceId = pictureEditByBatchRequest.getSpaceId();
        String category = pictureEditByBatchRequest.getCategory();
        List<String> tags = pictureEditByBatchRequest.getTags();
        // 校验空间权限
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        if (!loginUser.getId().equals(space.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间访问权限");
        }

        // 查询指定图片
        List<Picture> pictureList = this.lambdaQuery()
                .select(Picture::getId, Picture::getSpaceId)
                .eq(Picture::getSpaceId, spaceId)
                .in(Picture::getId, pictureIdList)
                .list();

        if (pictureList.isEmpty()) {
            return;
        }
        // 更新分类和标签
        pictureList.forEach(picture -> {
            log.info("更新图片分类和标签，id = {}", picture.getId());
            if (StrUtil.isNotBlank(category)) {
                picture.setCategory(category);
                log.info("更新图片分类: {}", category);
            }
            if (CollUtil.isNotEmpty(tags)) {
                picture.setTags(JSONUtil.toJsonStr(tags));
                log.info("更新图片分类: {}", tags);
            }
        });

        // 批量重命名
        String nameRule = pictureEditByBatchRequest.getNameRule();
        fillPictureWithNameRule(pictureList, nameRule);

        // 批量更新
        boolean result = this.updateBatchById(pictureList);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    /**
     * nameRule 格式：图片{序号}
     *
     * @param pictureList
     * @param nameRule
     */
    private void fillPictureWithNameRule(List<Picture> pictureList, String nameRule) {
        if (CollUtil.isEmpty(pictureList) || StrUtil.isBlank(nameRule)) {
            return;
        }
        long count = 1;
        try {
            for (Picture picture : pictureList) {
                String pictureName = nameRule.replaceAll("\\{序号}", String.valueOf(count++));
                picture.setName(pictureName);
            }
        } catch (Exception e) {
            log.error("名称解析错误", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "名称解析错误");
        }
    }



    @Async
    @Override
    public void clearPictureFile(Picture oldPicture) {
        // 判断该图片是否被多条记录使用
        String pictureUrl = oldPicture.getUrl();
        long count = this.lambdaQuery()
                .eq(Picture::getUrl, pictureUrl)
                .count();
        // 有不止一条记录用到了该图片，不清理
        if (count > 1) {
            return;
        }
        try {
            // 提取路径部分
            String picturePath = new URL(pictureUrl).getPath();
            cosManager.deleteObject(picturePath);

            // 清理缩略图
            String thumbnailUrl = oldPicture.getThumbnailUrl();
            if (StrUtil.isNotBlank(thumbnailUrl)) {
                String thumbnailPath = new URL(thumbnailUrl).getPath();
                cosManager.deleteObject(thumbnailPath);
            }
        } catch (MalformedURLException e) {
            log.error("处理图片删除时遇到格式错误的 URL。图片 URL: {}", pictureUrl, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"格式错误的 URL");
        }
    }

    @Override
    public List<PictureVO> searchPictureByColor(Long spaceId, String picColor) {
        User loginUser = (User) StpUtil.getSession().get("loginUser");
        // 校验空间权限
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        if (!space.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间访问权限");
        }
        // 查询该空间下的所有图片（必须要有主色调）
        List<Picture> pictureList = this.lambdaQuery()
                .eq(Picture::getSpaceId, spaceId)
                .isNotNull(Picture::getPicColor)
                .list();
        // 如果没有图片，直接返回空列表
        if (CollUtil.isEmpty(pictureList)) {
            return new ArrayList<>();
        }
        // 将颜色字符串转换为主色调
        Color targetColor = Color.decode(picColor);
        // 计算相似度并排序
        List<Picture> sortedPictureList = pictureList.stream()
                .sorted(Comparator.comparingDouble(picture -> {
                    String hexColor = picture.getPicColor();
                    // 没有主色调的图片会默认排序到最后
                    if (StrUtil.isBlank(hexColor)) {
                        return Double.MAX_VALUE;
                    }
                    Color pictureColor = Color.decode(hexColor);
                    // 计算相似度
                    // 越大越相似
                    return -ColorSimilarUtils.calculateSimilarity(targetColor, pictureColor);
                }))
                .limit(12) // 取前 12 个
                .collect(Collectors.toList());
        return sortedPictureList.stream()
                .map(PictureVO::objToVo)
                .collect(Collectors.toList());
    }
    @Override
    public CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest) {
        // 获取图片信息
       Long pictureId = createPictureOutPaintingTaskRequest.getPictureId();
       Picture picture = Optional.ofNullable(this.getById(pictureId)).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR));
       // 构造请求参数
       CreateOutPaintingTaskRequest taskRequest = new CreateOutPaintingTaskRequest();
       CreateOutPaintingTaskRequest.Input input = new CreateOutPaintingTaskRequest.Input();
       input.setImageUrl(picture.getUrl());
       taskRequest.setInput(input);
       BeanUtil.copyProperties(createPictureOutPaintingTaskRequest, taskRequest);
       // 创建任务
       return aliYunAiApi.createOutPaintingTask(taskRequest);
   }


}




