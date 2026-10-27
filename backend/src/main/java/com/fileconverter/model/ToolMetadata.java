package com.fileconverter.model;

import java.util.List;

public class ToolMetadata {
    private String id;
    private String name;
    private String category;
    private String description;
    private String icon;
    private String badge;
    private String route;
    private List<String> acceptedFormats;
    private List<String> outputFormats;
    private boolean isMultiFile;

    public ToolMetadata() {}

    public ToolMetadata(String id, String name, String category, String description, String icon, String badge, String route, List<String> acceptedFormats, List<String> outputFormats, boolean isMultiFile) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.description = description;
        this.icon = icon;
        this.badge = badge;
        this.route = route;
        this.acceptedFormats = acceptedFormats;
        this.outputFormats = outputFormats;
        this.isMultiFile = isMultiFile;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public String getBadge() { return badge; }
    public void setBadge(String badge) { this.badge = badge; }

    public String getRoute() { return route; }
    public void setRoute(String route) { this.route = route; }

    public List<String> getAcceptedFormats() { return acceptedFormats; }
    public void setAcceptedFormats(List<String> acceptedFormats) { this.acceptedFormats = acceptedFormats; }

    public List<String> getOutputFormats() { return outputFormats; }
    public void setOutputFormats(List<String> outputFormats) { this.outputFormats = outputFormats; }

    public boolean isMultiFile() { return isMultiFile; }
    public void setMultiFile(boolean multiFile) { isMultiFile = multiFile; }
}
