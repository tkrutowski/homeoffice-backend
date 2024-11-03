package net.focik.homeoffice.devices.domain.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.focik.homeoffice.finance.domain.firm.Firm;
import net.focik.homeoffice.utils.share.ActiveStatus;
import org.javamoney.moneta.Money;

import java.time.LocalDate;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class Device {
        private Integer id;
        private DeviceType deviceType;
        private Firm firm;
        private String name;
        private LocalDate purchaseDate;
        private Money purchaseAmount;
        private LocalDate sellDate;
        private Money sellAmount;
        private LocalDate warrantyEndDate;
        private LocalDate insuranceEndDate;
        private String otherInfo;
        private ActiveStatus activeStatus;
}
