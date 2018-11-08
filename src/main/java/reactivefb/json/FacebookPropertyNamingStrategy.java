package reactivefb.json;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.restfb.Facebook;

import static java.util.Optional.ofNullable;

public class FacebookPropertyNamingStrategy extends PropertyNamingStrategy {

    public static final PropertyNamingStrategy FACEBOOK_NAMING_STRATEGY = new FacebookPropertyNamingStrategy();

    @Override
    public String nameForField(MapperConfig<?> config, AnnotatedField field, String defaultName){

        return ofNullable(field.getAnnotation(Facebook.class))
                .map(this::nameFromAnnotation)
                .orElse(defaultName);
    }

    @Override
    public String nameForGetterMethod(MapperConfig<?> config, AnnotatedMethod method, String defaultName){
        return ofNullable(method.getAnnotation(Facebook.class))
                .map(this::nameFromAnnotation)
                .orElse(defaultName);
    }

    @Override
    public String nameForSetterMethod(MapperConfig<?> config, AnnotatedMethod method, String defaultName){
        return ofNullable(method.getAnnotation(Facebook.class))
                .map(this::nameFromAnnotation)
                .orElse(defaultName);
    }

    private String nameFromAnnotation(Facebook facebook){
        String name = facebook.value();
        if(name.isEmpty()){
            return null;
        }
        return name;
    }
}
