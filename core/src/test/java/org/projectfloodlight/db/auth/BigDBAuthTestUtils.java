package org.projectfloodlight.db.auth;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.junit.Test;
import org.projectfloodlight.core.module.FloodlightModuleContext;
import org.projectfloodlight.db.BigDBException;
import org.projectfloodlight.db.IBigDBService;
import org.projectfloodlight.db.MockBigDBService;
import org.projectfloodlight.db.auth.AuthConfig;
import org.projectfloodlight.db.auth.AuthContext;
import org.projectfloodlight.db.auth.BigDBGroup;
import org.projectfloodlight.db.auth.BigDBUser;
import org.projectfloodlight.db.auth.session.Session;
import org.projectfloodlight.db.auth.session.SimpleSessionManager;
import org.projectfloodlight.db.query.Query;
import org.projectfloodlight.db.service.Treespace;
import org.restlet.Request;
import org.restlet.data.Method;

import com.google.common.collect.ImmutableSet;

public class BigDBAuthTestUtils {
    public final static BigDBUser ADMIN_USER = new BigDBUser("admin", "full admin", ImmutableSet.of(new BigDBGroup("admin"), new BigDBGroup("reader")));
    public final static BigDBUser NON_ADMIN_USER = new BigDBUser("restricted", "restricted admin", ImmutableSet.of(new BigDBGroup("customgroup")));

    public final static Session MOCK_SESSION = new Session(1, "cookie", ADMIN_USER);
    static {
        MOCK_SESSION.setLastAddress("1.2.3.4");
    }
    public final static Request MOCK_REQUEST = new Request(Method.GET, "/test/resource");
    public final static AuthContext MOCK_AUTH_CONTEXT = AuthContext.forSession(MOCK_SESSION, MOCK_REQUEST);

    protected MockBigDBService bigDB;
    protected Treespace treespace;

    public void setUp() throws Exception {
        FloodlightModuleContext fmc = new FloodlightModuleContext();

        AuthConfig ac = new AuthConfig();
        ac.setParam(AuthConfig.AUTH_ENABLED, true);
        ac.setParam(AuthConfig.SESSION_MANAGER, SimpleSessionManager.class);

        bigDB = new MockBigDBService();
        bigDB.addModuleSchema("floodlight", "2012-10-22");
        bigDB.addModuleSchema("aaa");
        bigDB.setAuthConfig(ac);
        bigDB.init(fmc);
        fmc.addService(IBigDBService.class, bigDB);

        // start up the auth service. This will bind the dynamic data hooks to the treespace. We don't run/boot the rest service
        // for now
        bigDB.getService().getAuthService().startUp();

        treespace = bigDB.getControllerTreespace();
    }

    @Test
    public void pseudoTest() {
         // no op
    }

    public void createUser(BigDBUser user, String password)
            throws BigDBException, UnsupportedEncodingException {
        // Add users
        Query query;
        String insertData;
        InputStream input;

        query = Query.parse("/core/aaa/local-user");
        insertData = "[{" +
                "\"user-name\":\"" + user.getUser() + "\"," +
                "\"password\": \" + " + password + "\"," +
                "\"full-name\":\"" + user.getFullName() + "\"" +
                "}]";
        input = new ByteArrayInputStream(insertData.getBytes("UTF-8"));
        treespace.insertData(query, Treespace.DataFormat.JSON, input,
                              AuthContext.SYSTEM);
    }


}
