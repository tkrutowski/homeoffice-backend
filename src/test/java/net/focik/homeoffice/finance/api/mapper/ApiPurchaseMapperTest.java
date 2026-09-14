package net.focik.homeoffice.finance.api.mapper;

import net.focik.homeoffice.finance.api.dto.PurchaseDto;
import net.focik.homeoffice.finance.domain.purchase.Purchase;
import net.focik.homeoffice.utils.share.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Skupiony na mapowaniu Purchase.idLoan (dodane przy okazji ConvertPurchasesToLoanUseCase) -
 * latwo pominac to pole przy rozbudowie mapperow, bo sa budowane recznie (bez ModelMapper STRICT).
 */
class ApiPurchaseMapperTest {

    private final ApiPurchaseMapper mapper = new ApiPurchaseMapper();

    @Test
    @DisplayName("should map idLoan to the DTO when the purchase has been converted to a loan")
    void toDto_ShouldMapIdLoan_WhenPurchaseIsConverted() {
        Purchase purchase = Purchase.builder()
                .id(1)
                .idUser(10)
                .idCard(1)
                .idFirm(1)
                .name("zakup")
                .amount(new BigDecimal("100.00"))
                .purchaseDate(LocalDate.now())
                .paymentStatus(PaymentStatus.CONVERTED)
                .idLoan(99)
                .build();

        PurchaseDto dto = mapper.toDto(purchase);

        assertThat(dto.getIdLoan()).isEqualTo(99);
    }

    @Test
    @DisplayName("should leave idLoan null in the DTO when the purchase has not been converted")
    void toDto_ShouldLeaveIdLoanNull_WhenPurchaseIsNotConverted() {
        Purchase purchase = Purchase.builder()
                .id(1)
                .idUser(10)
                .name("zakup")
                .amount(new BigDecimal("100.00"))
                .purchaseDate(LocalDate.now())
                .paymentStatus(PaymentStatus.TO_PAY)
                .build();

        PurchaseDto dto = mapper.toDto(purchase);

        assertThat(dto.getIdLoan()).isNull();
    }

    @Test
    @DisplayName("should map idLoan from the DTO back to the domain object")
    void toDomain_ShouldMapIdLoan() {
        PurchaseDto dto = PurchaseDto.builder()
                .id(1)
                .idUser(10)
                .name("zakup")
                .amount("100.00")
                .purchaseDate(LocalDate.now())
                .paymentStatus(PaymentStatus.CONVERTED)
                .idLoan(99)
                .build();

        Purchase purchase = mapper.toDomain(dto);

        assertThat(purchase.getIdLoan()).isEqualTo(99);
    }
}
