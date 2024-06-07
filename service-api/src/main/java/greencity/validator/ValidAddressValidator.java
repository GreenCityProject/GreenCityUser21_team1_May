package greencity.validator;

import greencity.annotations.ValidAddress;
import greencity.dto.event.EventSaveDayInfoDto;
import greencity.dto.event.model.EventStatus;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;
import java.util.List;

public class ValidAddressValidator implements ConstraintValidator<ValidAddress, List<EventSaveDayInfoDto>> {
    @Override
    public boolean isValid(List<EventSaveDayInfoDto> value, ConstraintValidatorContext context) {
        for(EventSaveDayInfoDto day: value) {
            if (day.getStatus().equals(EventStatus.OFFLINE) || day.getStatus().equals(EventStatus.ONLINE_OFFLINE)) {
                if (day.getAddress() == null) {
                    return false;
                }
                if (day.getAddress().getLatitude() == null || day.getAddress().getLongitude() == null) {
                    return false;
                }
                if (day.getAddress().getLatitude().compareTo(BigDecimal.valueOf(90)) > 0 ||
                        day.getAddress().getLatitude().compareTo(BigDecimal.valueOf(-90)) < 0 ) {
                    return false;
                }
                if (day.getAddress().getLongitude().compareTo(BigDecimal.valueOf(180)) > 0 ||
                        day.getAddress().getLongitude().compareTo(BigDecimal.valueOf(-180)) < 0 ) {
                    return false;
                }
                if ((day.getAddress().getAddressEn() == null || day.getAddress().getAddressEn().isBlank()) &&
                        (day.getAddress().getAddressUa() == null || day.getAddress().getAddressUa().isBlank())) {
                    return false;
                }
            } else if (day.getAddress() != null) {
                return false;
            }
        }
        return true;
    }
}
