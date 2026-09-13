package net.focik.homeoffice.finance.infrastructure.mapper;

import net.focik.homeoffice.finance.domain.purchase.Purchase;
import net.focik.homeoffice.finance.infrastructure.dto.PurchaseDbDto;
import net.focik.homeoffice.utils.share.PaymentStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Skupiony na mapowaniu Purchase.idLoan (dodane przy okazji ConvertPurchasesToLoanUseCase).
 */
class JpaPurchaseMapperTest {

    private final JpaPurchaseMapper mapper = new JpaPurchaseMapper();

    @Test
    void toDto_ShouldMapIdLoan() {
        Purchase purchase = Purchase.builder()
                .id(1)
                .idUser(10)
                .name("zakup")
                .amount(new BigDecimal("100.00"))
                .purchaseDate(LocalDate.now())
                .paymentStatus(PaymentStatus.CONVERTED)
                .idLoan(99)
                .build();

        PurchaseDbDto dto = mapper.toDto(purchase);

        assertThat(dto.getIdLoan()).isEqualTo(99);
    }

    @Test
    void toDomain_ShouldMapIdLoan() {
        PurchaseDbDto dto = PurchaseDbDto.builder()
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

        Purchase purchase = mapper.toDomain(dto);

        assertThat(purchase.getIdLoan()).isEqualTo(99);
    }
}
