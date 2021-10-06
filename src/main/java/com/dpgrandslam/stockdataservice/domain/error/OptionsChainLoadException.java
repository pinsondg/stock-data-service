package com.dpgrandslam.stockdataservice.domain.error;

public class OptionsChainLoadException extends Exception {

    private String ticker;
    private String sourceUrl;

    public OptionsChainLoadException(String ticker, String sourceUrl, String message, Throwable cause) {
        super(message, cause);
        this.ticker = ticker;
        this.sourceUrl = sourceUrl;
    }

    public OptionsChainLoadException(String ticker, String sourceUrl, String message) {
        super(message);
        this.sourceUrl = sourceUrl;
        this.ticker = ticker;
    }

    @Override
    public String getMessage() {
        return ("Could not load options chain for ticker " + ticker + " at URL " + sourceUrl + ". " + super.getMessage()).trim();
    }
}
