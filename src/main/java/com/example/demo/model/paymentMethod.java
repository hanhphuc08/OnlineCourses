package com.example.demo.model;

public class paymentMethod {
	
	private int paymentMethodID;
    private int paymentID;
    private String methodType; 
    private String eWalletProvider;
    private Long eWalletTransactionID;
	public int getPaymentMethodID() {
		return paymentMethodID;
	}
	public void setPaymentMethodID(int paymentMethodID) {
		this.paymentMethodID = paymentMethodID;
	}
	public int getPaymentID() {
		return paymentID;
	}
	public void setPaymentID(int paymentID) {
		this.paymentID = paymentID;
	}
	public String getMethodType() {
		return methodType;
	}
	public void setMethodType(String methodType) {
		this.methodType = methodType;
	}
	public String geteWalletProvider() {
		return eWalletProvider;
	}
	public void seteWalletProvider(String eWalletProvider) {
		this.eWalletProvider = eWalletProvider;
	}
	public Long geteWalletTransactionID() {
		return eWalletTransactionID;
	}
	public void seteWalletTransactionID(Long eWalletTransactionID) {
		this.eWalletTransactionID = eWalletTransactionID;
	}
	
    
    

}
