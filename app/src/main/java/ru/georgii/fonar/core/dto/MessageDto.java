package ru.georgii.fonar.core.dto;

public class MessageDto {

    public final String text;
    public String type = "plain";

    public MessageDto(String text) {
        this.text = text;
    }

}
