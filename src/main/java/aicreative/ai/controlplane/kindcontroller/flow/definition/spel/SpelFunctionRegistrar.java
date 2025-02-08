package aicreative.ai.controlplane.kindcontroller.flow.definition.spel;

import aicreative.ai.common.oss.OssHandler;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.expression.spel.support.ReflectiveMethodResolver;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class SpelFunctionRegistrar {
    @Resource
    private OssHandler ossHandler;

    public void registerFunctions(StandardEvaluationContext context) {
        context.addMethodResolver(new ReflectiveMethodResolver());
        setFunctionVariable(context, Fn.class);
        setFunctionVariable(context, ossHandler);
        setFunctionVariable(context, StringUtils.class);
        setFunctionVariable(context, Integer.class);
        setFunctionVariable(context, Math.class);
        setFunctionVariable(context, List.class);
        setFunctionVariable(context, Map.class);
    }

    private void setFunctionVariable(StandardEvaluationContext context, Object f) {
        if (f instanceof Class<?>) {
            context.setVariable(((Class<?>) f).getSimpleName(), f);
        } else {
            context.setVariable(f.getClass().getSimpleName(), f);
        }
    }
}
