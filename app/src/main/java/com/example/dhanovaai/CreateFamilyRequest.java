// CreateFamilyRequest.java
package com.example.dhanovaai;
import java.util.List;

public class CreateFamilyRequest {
    public String family_name;
    public List<String> member_emails;

    public CreateFamilyRequest(String family_name, List<String> member_emails) {
        this.family_name = family_name;
        this.member_emails = member_emails;
    }
}