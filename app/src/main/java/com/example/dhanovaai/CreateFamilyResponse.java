// CreateFamilyResponse.java
package com.example.dhanovaai;
import java.util.List;

public class CreateFamilyResponse {
    public String message;
    public int family_id;
    public String family_name;
    public List<String> invited;
    public List<String> skipped_not_registered;
}