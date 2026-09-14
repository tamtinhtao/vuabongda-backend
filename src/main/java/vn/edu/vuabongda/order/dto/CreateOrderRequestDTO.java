package vn.edu.vuabongda.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateOrderRequestDTO {

    @NotBlank(
            message = "Ten nguoi nhan khong duoc de trong"
    )
    private String recipientName;

    @NotBlank(
            message = "So dien thoai khong duoc de trong"
    )
    private String phone;

    @NotBlank(
            message = "Dia chi giao hang khong duoc de trong"
    )
    private String shippingAddress;
}