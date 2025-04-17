package com.tune.picturebackend.api;

// 1. 策略接口
interface PaymentStrategy {
    void pay(double amount);
}

// 2. 具体策略类 1 - 信用卡支付
class CreditCardPayment implements PaymentStrategy {
    private String cardNumber;
    private String expirationDate;
    private String cvv;

    public CreditCardPayment(String cardNumber, String expirationDate, String cvv) {
        this.cardNumber = cardNumber;
        this.expirationDate = expirationDate;
        this.cvv = cvv;
    }

    @Override
    public void pay(double amount) {
        System.out.println("支付金额: $" + amount);
        System.out.println("使用信用卡支付，卡号: " + cardNumber + "，有效期: " + expirationDate + "，CVV: " + cvv);
    }
}

// 3. 具体策略类 2 - PayPal 支付
class PayPalPayment implements PaymentStrategy {
    private String email;
    private String password;

    public PayPalPayment(String email, String password) {
        this.email = email;
        this.password = password;
    }

    @Override
    public void pay(double amount) {
        System.out.println("支付金额: $" + amount);
        System.out.println("使用PayPal支付，邮箱: " + email);
    }
}

// 4. 上下文类
class PaymentContext {
    private PaymentStrategy paymentStrategy;

    public PaymentContext(PaymentStrategy paymentStrategy) {
        this.paymentStrategy = paymentStrategy;
    }

    public void setPaymentStrategy(PaymentStrategy paymentStrategy) {
        this.paymentStrategy = paymentStrategy;
    }

    public void executePayment(double amount) {
        paymentStrategy.pay(amount);
    }
}

// 5. 客户端代码
public class StrategyPatternDemo {
    public static void main(String[] args) {
        // 创建不同的支付策略
        PaymentStrategy creditCardPayment = new CreditCardPayment("1234-5678-9012-3456", "12/25", "123");
        PaymentStrategy paypalPayment = new PayPalPayment("user@example.com", "password");

        // 使用上下文执行支付
        PaymentContext context = new PaymentContext(creditCardPayment);
        context.executePayment(100.0);

        System.out.println("\n切换支付方式到 PayPal:");
        context.setPaymentStrategy(paypalPayment);
        context.executePayment(200.0);
    }
}