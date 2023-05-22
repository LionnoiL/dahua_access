package ua.gaponov.database;

public class Event {
    private String cardNo;
    private String date;

    public Event(String cardNo, String date) {
        this.cardNo = cardNo;
        this.date = date;
    }

    public String getCardNo() {
        return cardNo;
    }

    public String getDate() {
        return date;
    }
}
