package ru.georgii.fonar.core.dto;

public class ServerConfigDto {

    public String server;

    public String server_software_name;

    public String server_version;

    public String api_spec;

    public String api_version;

    public String server_name;

    public String salt;

    public String socketUrl;

    public String getUrl() {
        return socketUrl;
    }

}
