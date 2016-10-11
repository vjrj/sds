package au.org.ala.sds.model;


public interface Message {

    enum Type {
        ERROR, WARNING, ALERT, INFO
    }

    Type getType();
    String getMessageText();
    String getCategory();
}
