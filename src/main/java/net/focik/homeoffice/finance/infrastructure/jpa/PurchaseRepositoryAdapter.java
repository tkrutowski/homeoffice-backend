package net.focik.homeoffice.finance.infrastructure.jpa;

import lombok.AllArgsConstructor;
import net.focik.homeoffice.finance.domain.purchase.Purchase;
import net.focik.homeoffice.finance.domain.purchase.port.secondary.PurchaseRepository;
import net.focik.homeoffice.finance.infrastructure.dto.PurchaseDbDto;
import net.focik.homeoffice.finance.infrastructure.mapper.JpaPurchaseMapper;
import net.focik.homeoffice.utils.share.PaymentStatus;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Primary
@AllArgsConstructor
class PurchaseRepositoryAdapter implements PurchaseRepository {

    PurchaseDtoRepository purchaseDtoRepository;
    LoanInstallmentDtoRepository loanInstallmentDtoRepository;
    JpaPurchaseMapper mapper;


    @Override
    public Purchase savePurchase(Purchase purchase) {
        if (purchase.getId() == 0){
            purchase.setId(null);
        }
        PurchaseDbDto saved = purchaseDtoRepository.save(mapper.toDto(purchase));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Purchase> findPurchaseById(Integer id) {
        Optional<PurchaseDbDto> byId = purchaseDtoRepository.findById(id);
        return byId.map(purchaseDbDto -> mapper.toDomain(purchaseDbDto));
    }

    @Override
    public List<Purchase> findPurchaseByUserId(Integer idUser) {
        return purchaseDtoRepository.findAllByIdUser(idUser).stream()
                .map(loanDto -> mapper.toDomain(loanDto))
                .collect(Collectors.toList());
    }

    @Override
    public List<Purchase> findPurchaseByUserAndStatus(Integer idUser, PaymentStatus status) {
        return purchaseDtoRepository.findAllByIdUserAndPaymentStatus(idUser, status).stream()
                .map(loanDto -> mapper.toDomain(loanDto))
                .collect(Collectors.toList());
    }

    @Override
    public List<Purchase> findPurchaseByUserIdAndDeadline(Integer idUser, LocalDate deadline) {
        return purchaseDtoRepository.findAllByIdUserAndPaymentDeadline(idUser, deadline).stream()
                .map(loanDto -> mapper.toDomain(loanDto))
                .collect(Collectors.toList());
    }

    @Override
    public List<Purchase> findAll() {
        return purchaseDtoRepository.findAll().stream()
                .map(loanDto -> mapper.toDomain(loanDto))
                .collect(Collectors.toList());
    }

    @Override
    public List<Purchase> findAllByCard(Integer idCard) {
        return purchaseDtoRepository.findAllByIdCard(idCard).stream()
                .map(loanDto -> mapper.toDomain(loanDto))
                .collect(Collectors.toList());
    }

    @Override
    public List<Purchase> findAllByFirm(Integer idFirm) {
        return purchaseDtoRepository.findAllByIdFirm(idFirm).stream()
                .map(loanDto -> mapper.toDomain(loanDto))
                .collect(Collectors.toList());
    }

    @Override
    public void deletePurchaseById(int idLoan) {
        purchaseDtoRepository.deleteById(idLoan);
    }

}