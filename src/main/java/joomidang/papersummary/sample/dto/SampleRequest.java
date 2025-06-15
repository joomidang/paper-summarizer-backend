package joomidang.papersummary.sample.dto;

public class SampleRequest {
    private String name;
    private String description;
    private String category;

    // Default constructor for JSON deserialization
    public SampleRequest() {
    }

    public SampleRequest(String name, String description, String category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}