package com.dpgrandslam.stockdataservice.domain.model.options;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode
@Getter
@AllArgsConstructor
@Builder
public class OptionChainKey {

    public Double strike;
    public Option.OptionType optionType;

}
