package com.freshcart.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CheckoutRequest {

    @NotBlank(message = "Phone is required")
    @Size(max = 16, message = "Phone cannot exceed 16 characters")
    @Pattern(
        regexp = "^\+?[0-9]{10,15}$",
        message = "Phone must contain 10 to 15 digits, with an optional + prefix"
    )
    private String phone;

    @NotBlank(message = "Address is required")
    @Size(max = 300, message = "Address cannot exceed 300 characters")
    private String address;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City cannot exceed 100 characters")
    private String city;

    @NotBlank(message = "State is required")
    @Size(max = 100, message = "State cannot exceed 100 characters")
    private String state;

    @NotBlank(message = "Pincode is required")
    @Pattern(
        regexp = "^[1-9][0-9]{5}$",
        message = "Pincode must be a valid 6 digit code"
    )
    private String pincode;

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }
}
