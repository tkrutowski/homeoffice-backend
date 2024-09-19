package net.focik.homeoffice.library.domain.model;


public enum WebSite {
    UPOLUJ_EBOOKA("upolujebooka.pl"), LEGIMI("legimi.pl"), LUBIMY_CZYTAC("lubimyczytac.pl"), CHAT_GPT("open.ai");

    private final String url;

    WebSite(String url) {
        this.url = url;
    }

    public String getUrl() {
        return this.url;
    }
}
