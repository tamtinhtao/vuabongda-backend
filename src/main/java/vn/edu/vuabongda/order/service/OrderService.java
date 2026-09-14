package vn.edu.vuabongda.order.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.vuabongda.cart.entity.Cart;
import vn.edu.vuabongda.cart.entity.CartItem;
import vn.edu.vuabongda.cart.repository.CartItemRepository;
import vn.edu.vuabongda.cart.repository.CartRepository;
import vn.edu.vuabongda.order.dto.CreateOrderRequestDTO;
import vn.edu.vuabongda.order.dto.OrderItemResponseDTO;
import vn.edu.vuabongda.order.dto.OrderResponseDTO;
import vn.edu.vuabongda.order.entity.Order;
import vn.edu.vuabongda.order.entity.OrderItem;
import vn.edu.vuabongda.order.repository.OrderItemRepository;
import vn.edu.vuabongda.order.repository.OrderRepository;
import vn.edu.vuabongda.product.entity.Product;
import vn.edu.vuabongda.product.repository.ProductRepository;
import vn.edu.vuabongda.user.entity.User;
import vn.edu.vuabongda.user.repository.UserRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;

    private final OrderItemRepository orderItemRepository;

    private final UserRepository userRepository;

    private final CartRepository cartRepository;

    private final CartItemRepository cartItemRepository;

    private final ProductRepository productRepository;

    // ================================
    // DAT HANG TU CART
    // ================================
    public OrderResponseDTO createOrder(
            String username,
            CreateOrderRequestDTO request
    ) {

        User user = userRepository
                .findByUsername(username)
                .orElseThrow(
                        () -> new NoSuchElementException(
                                "Khong tim thay nguoi dung"
                        )
                );

        Cart cart = cartRepository
                .findByUserId(user.getId())
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "Gio hang dang trong"
                        )
                );

        List<CartItem> cartItems =
                cartItemRepository.findByCartId(
                        cart.getId()
                );

        if (cartItems.isEmpty()) {
            throw new IllegalArgumentException(
                    "Gio hang dang trong"
            );
        }

        // ================================
        // KIEM TRA TON KHO + TINH TONG
        // ================================
        BigDecimal totalAmount =
                BigDecimal.ZERO;

        for (CartItem cartItem : cartItems) {

            Product product =
                    cartItem.getProduct();

            if (!"ACTIVE".equalsIgnoreCase(
                    product.getStatus()
            )) {

                throw new IllegalArgumentException(
                        "San pham "
                                + product.getName()
                                + " hien khong hoat dong"
                );
            }

            if (cartItem.getQuantity()
                    > product.getStockQuantity()) {

                throw new IllegalArgumentException(
                        "San pham "
                                + product.getName()
                                + " khong du ton kho"
                );
            }

            BigDecimal subtotal =
                    product.getPrice()
                            .multiply(
                                    BigDecimal.valueOf(
                                            cartItem.getQuantity()
                                    )
                            );

            totalAmount =
                    totalAmount.add(subtotal);
        }

        // ================================
        // TAO ORDER
        // ================================
        Order order = new Order();

        order.setUser(user);

        order.setRecipientName(
                request.getRecipientName()
        );

        order.setPhone(
                request.getPhone()
        );

        order.setShippingAddress(
                request.getShippingAddress()
        );

        order.setTotalAmount(totalAmount);

        order.setStatus("PENDING");

        Order savedOrder =
                orderRepository.save(order);

        // ================================
        // TAO ORDER ITEMS + TRU TON KHO
        // ================================
        List<OrderItem> orderItems =
                new ArrayList<>();

        for (CartItem cartItem : cartItems) {

            Product product =
                    cartItem.getProduct();

            BigDecimal unitPrice =
                    product.getPrice();

            BigDecimal subtotal =
                    unitPrice.multiply(
                            BigDecimal.valueOf(
                                    cartItem.getQuantity()
                            )
                    );

            OrderItem orderItem =
                    new OrderItem();

            orderItem.setOrder(savedOrder);

            orderItem.setProduct(product);

            orderItem.setProductName(
                    product.getName()
            );

            orderItem.setUnitPrice(
                    unitPrice
            );

            orderItem.setQuantity(
                    cartItem.getQuantity()
            );

            orderItem.setSubtotal(
                    subtotal
            );

            orderItems.add(orderItem);

            // TRU TON KHO
            product.setStockQuantity(
                    product.getStockQuantity()
                            - cartItem.getQuantity()
            );

            productRepository.save(product);
        }

        orderItemRepository.saveAll(
                orderItems
        );

        // ================================
        // XOA CART ITEMS SAU KHI DAT HANG
        // ================================
        cartItemRepository.deleteAllByCartId(
                cart.getId()
        );

        return toDTO(
                savedOrder,
                orderItems
        );
    }

    // ================================
    // LAY DANH SACH DON CUA USER
    // ================================
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getMyOrders(
            String username
    ) {

        User user = userRepository
                .findByUsername(username)
                .orElseThrow(
                        () -> new NoSuchElementException(
                                "Khong tim thay nguoi dung"
                        )
                );

        return orderRepository
                .findByUserIdOrderByCreatedAtDesc(
                        user.getId()
                )
                .stream()
                .map(this::toDTO)
                .toList();
    }

    // ================================
    // XEM CHI TIET 1 DON
    // ================================
    @Transactional(readOnly = true)
    public OrderResponseDTO getMyOrderById(
            String username,
            Long orderId
    ) {

        User user = userRepository
                .findByUsername(username)
                .orElseThrow(
                        () -> new NoSuchElementException(
                                "Khong tim thay nguoi dung"
                        )
                );

        Order order = orderRepository
                .findByIdAndUserId(
                        orderId,
                        user.getId()
                )
                .orElseThrow(
                        () -> new NoSuchElementException(
                                "Khong tim thay don hang"
                        )
                );

        return toDTO(order);
    }

    // ================================
    // CHUYEN ORDER -> DTO
    // ================================
    private OrderResponseDTO toDTO(
            Order order
    ) {

        List<OrderItem> items =
                orderItemRepository.findByOrderId(
                        order.getId()
                );

        return toDTO(order, items);
    }

    private OrderResponseDTO toDTO(
            Order order,
            List<OrderItem> items
    ) {

        List<OrderItemResponseDTO> itemDTOs =
                items.stream()
                        .map(this::toItemDTO)
                        .toList();

        return new OrderResponseDTO(
                order.getId(),
                order.getUser().getId(),
                order.getRecipientName(),
                order.getPhone(),
                order.getShippingAddress(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getCreatedAt(),
                itemDTOs
        );
    }

    private OrderItemResponseDTO toItemDTO(
            OrderItem item
    ) {

        return new OrderItemResponseDTO(
                item.getId(),
                item.getProduct().getId(),
                item.getProductName(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getSubtotal()
        );
    }
}