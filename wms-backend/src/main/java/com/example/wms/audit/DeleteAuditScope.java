package com.example.wms.audit;

// 删除审计上下文：在当前请求范围内绑定删除原因
public final class DeleteAuditScope implements AutoCloseable {
    private final RequestAuditContext context;
    private final String previousReason;
    private final boolean createdContext;

    private DeleteAuditScope(String reason) {
        RequestAuditContext existing = RequestAuditContext.get();
        if (existing == null) {
            existing = new RequestAuditContext();
            RequestAuditContext.set(existing);
            this.createdContext = true;
        } else {
            this.createdContext = false;
        }
        this.context = existing;
        this.previousReason = existing.getDeleteReason();
        existing.setDeleteReason(reason == null ? null : reason.trim());
    }

    public static DeleteAuditScope bind(String reason) {
        return new DeleteAuditScope(reason);
    }

    @Override
    public void close() {
        if (context != null) {
            context.setDeleteReason(previousReason);
        }
        if (createdContext) {
            RequestAuditContext.clear();
        }
    }
}
