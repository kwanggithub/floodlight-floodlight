package org.projectfloodlight.db.yang;

public interface Describable {
    public String getDescription();
    public String getReference();
    
    public void setDescription(String description);
    public void setReference(String reference);
}
