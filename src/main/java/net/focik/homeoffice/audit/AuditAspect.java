package net.focik.homeoffice.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.focik.homeoffice.utils.UserHelper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Around("@annotation(auditLog)")
    public Object auditMethod(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        Object result;
        try {
            result = joinPoint.proceed();

            String entityId = extractEntityId(joinPoint, result, auditLog.action());
            String newValuesJson = serializeToJson(result);

            auditService.log(auditLog.entityType(), entityId, auditLog.action(), newValuesJson, getCurrentAuditor());

            return result;
        } catch (Throwable e) {
            throw e;
        }
    }

    private String getCurrentAuditor() {
        try {
            return UserHelper.getUserName();
        } catch (Exception e) {
            // If no user (async context), try to get job type
            return AsyncContext.getJobType();
        }
    }

    private String extractEntityId(ProceedingJoinPoint joinPoint, Object result, AuditAction action) {
        try {
            Object target = null;
            if (action == AuditAction.CREATE || action == AuditAction.UPDATE) {
                target = result;
            } else if (action == AuditAction.DELETE) {
                target = joinPoint.getArgs().length > 0 ? joinPoint.getArgs()[0] : result;
            }

            if (target != null) {
                Object id = getIdFromObject(target);
                return id != null ? id.toString() : "unknown";
            }
        } catch (Exception e) {
            log.warn("Failed to extract entity ID: {}", e.getMessage());
        }
        return "unknown";
    }

    private Object getIdFromObject(Object obj) {
        try {
            var method = obj.getClass().getMethod("getId");
            return method.invoke(obj);
        } catch (NoSuchMethodException | IllegalAccessException | java.lang.reflect.InvocationTargetException e) {
            // Try with reflection on field
            try {
                var field = obj.getClass().getDeclaredField("id");
                field.setAccessible(true);
                return field.get(obj);
            } catch (Exception ex) {
                return null;
            }
        }
    }

    private String serializeToJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("Failed to serialize object to JSON: {}", e.getMessage());
            return null;
        }
    }
}
