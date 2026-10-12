// SetPermissionsRequest.java
package com.example.dhanovaai;
import java.util.List;

public class SetPermissionsRequest {
    public List<String> permissions;

    public SetPermissionsRequest(List<String> permissions) {
        this.permissions = permissions;
    }
}