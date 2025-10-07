package org.nahap.digital_library_backend.exception;

public class RoleNotFoundException extends  RuntimeException{
    public RoleNotFoundException(String message){
        super(message);
    }
}
