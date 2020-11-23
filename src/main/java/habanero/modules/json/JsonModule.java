package habanero.modules.json;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import habanero.app.requests.ValidatedRequest;
import habanero.exceptions.HabaneroBusinessException;
import io.jooby.Context;
import io.jooby.MediaType;
import io.jooby.StatusCode;
import io.jooby.json.GsonModule;
import kotlin.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * JoobyのGsonModuleに、リクエストのバリデーションを自動的に行うようにした拡張クラス
 * <p>
 * Kotlinで実装するとリクエストがnullの場合に例外を出したり、
 * レスポンスがnullのとき実際のレスポンスが"{}"になるため、
 * Javaで実装する。
 *
 * @author Ryutaro Akiya
 */
public class JsonModule extends GsonModule {

    private final static Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private final static Log logger = LogFactory.getLog(JsonModule.class);

    public JsonModule(Gson gson) {
        super(gson);
    }

    /**
     * JSONをオブジェクトに変換して、バリデーションを行う。
     */
    @NotNull
    @Override
    public Object decode(@NotNull Context ctx, @NotNull Type type) throws Exception {
        try {
            // JSONからオブジェクトへ変換
            Object request = super.decode(ctx, type);

            logger.debug(request);

            // バリデーション
            if (request instanceof ValidatedRequest) {
                Set<ConstraintViolation<Object>> validation = validator.validate(request);

                if (validation.size() != 0)
                    throw ((ValidatedRequest) request).toException(validation);
            }

            return request;

        } catch (JsonIOException | JsonSyntaxException e) {
            List<String> messages = new ArrayList<>();
            messages.add("不正な形式です");

            throw new HabaneroBusinessException(e)
                    .statusCode(StatusCode.BAD_REQUEST)
                    .because(new Pair("body", messages));
        }
    }

    /**
     * レスポンスをJSONに変換する。レスポンスがnullの場合は空文字をレスポンスにする。
     */
    @NotNull
    @Override
    public byte[] encode(@NotNull Context ctx, @NotNull Object value) {
        if (value == null) { // レスポンスがNull
            ctx.setDefaultResponseType(MediaType.json);
            return new byte[0];
        } else {
            return super.encode(ctx, value);
        }
    }
}
