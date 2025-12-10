package net.focik.homeoffice.goahead.infrastructure.jpa;

import net.focik.homeoffice.goahead.infrastructure.dto.InvoiceDbDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

interface InvoiceDtoRepository extends JpaRepository<InvoiceDbDto, Integer>, JpaSpecificationExecutor<InvoiceDbDto> {

    List<InvoiceDbDto> findAllByCustomer_Id(Integer customerId);

    Optional<InvoiceDbDto> findByNumber(String number);

    List<InvoiceDbDto> findInvoiceDbDtosByNumberContainsOrderByNumberDesc(String number);

    /**
     * Pobiera statystyki miesięcznych sum faktur zgrupowane po roku i miesiącu.
     * Zwraca tablicę obiektów zawierającą: [rok, miesiąc, suma]
     */
    @Query(value = "SELECT YEAR(i.sell_date) as year, MONTH(i.sell_date) as month, " +
            "SUM(item.amount * item.quantity) as total " +
            "FROM goahead_invoice i " +
            "JOIN goahead_invoice_item item ON i.id = item.id_invoice " +
            "GROUP BY YEAR(i.sell_date), MONTH(i.sell_date) " +
            "ORDER BY year, month", nativeQuery = true)
    List<Object[]> findByMonthlyAmountStats();

    /**
     * Pobiera statystyki miesięcznych sum faktur dla konkretnego roku z podziałem na klientów.
     * Zwraca tablicę obiektów zawierającą: [id_klienta, miesiąc, suma]
     */
    @Query(value = "SELECT i.id_customer as customerId, MONTH(i.sell_date) as month, " +
            "SUM(item.amount * item.quantity) as total " +
            "FROM goahead_invoice i " +
            "JOIN goahead_invoice_item item ON i.id = item.id_invoice " +
            "WHERE YEAR(i.sell_date) = :year " +
            "GROUP BY i.id_customer, MONTH(i.sell_date) " +
            "ORDER BY customerId, month", nativeQuery = true)
    List<Object[]> findByMonthlyAmountStatsByYearAndCustomer(@Param("year") Integer year);
}
