package filters;

import play.Logger;
import play.api.mvc.RequestHeader;
import play.filters.csrf.CSRF.TokenProvider;
import play.filters.csrf.CSRFAction$;
import play.filters.csrf.CSRFConf$;
import play.libs.F;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.SimpleResult;
import scala.Option;

public class RequireCSRFCheck4NgAction extends Action<RequireCSRFCheck4Ng> {

    private final String tokenName = CSRFConf$.MODULE$.TokenName();
    private final Option<String> cookieName = CSRFConf$.MODULE$.CookieName();
    private final CSRFAction$ CSRFAction = CSRFAction$.MODULE$;
    private final TokenProvider tokenProvider = CSRFConf$.MODULE$.defaultTokenProvider();
    @Override
    public F.Promise<SimpleResult> call(Http.Context ctx) throws Throwable {
        RequestHeader request = ctx._requestHeader();

        if (CSRFAction.checkCsrfBypass(request)) {
            Logger.debug("bypassed");
            return delegate.call(ctx);
        }

        Option<String> headerToken = CSRFAction.getTokenFromHeader(request, tokenName, cookieName);

        if (!headerToken.isDefined()) {
            return F.Promise.pure((SimpleResult) forbidden("CSRF token by Server not found"));
        }

        Option<String> ngToken = request.headers().get("X-XSRF-TOKEN");

        if (!ngToken.isDefined()) {
            return F.Promise.pure((SimpleResult) forbidden("CSRF token by AngularJS not found"));
        }

        if (tokenProvider.compareTokens(headerToken.get(), ngToken.get())) {
            return delegate.call(ctx);
        }

        return F.Promise.pure((SimpleResult) forbidden("CSRF tokens don't match"));
    }
}
