package com.tpouade.multiteatro.security;

import java.util.Arrays;

import org.springframework.stereotype.Component;

import com.tpouade.multiteatro.exception.AccesoDenegadoException;
import com.tpouade.multiteatro.model.Teatro;
import com.tpouade.multiteatro.model.Usuario;

@Component
public class AutorizacionService {

    public void requiereRol(Usuario actual, Usuario.Rol... rolesPermitidos) {
        if (actual.getRol() == Usuario.Rol.SUPERADMIN) return;
        boolean tienePermiso = Arrays.asList(rolesPermitidos).contains(actual.getRol());
        if (!tienePermiso) {
            throw new AccesoDenegadoException("No tenés permisos para esta acción");
        }
    }

    public void requiereAdminDeTeatro(Usuario actual, Teatro teatro) {
        if (actual.getRol() == Usuario.Rol.SUPERADMIN) return;
        if (actual.getRol() != Usuario.Rol.ADMIN) {
            throw new AccesoDenegadoException("Solo un Admin puede realizar esta acción");
        }
        if (teatro.getAdmin() == null || !teatro.getAdmin().getId().equals(actual.getId())) {
            throw new AccesoDenegadoException("No administrás este teatro");
        }
    }
}