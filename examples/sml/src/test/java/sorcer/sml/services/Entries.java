package sorcer.sml.services;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.arithmetic.provider.impl.AdderImpl;
import sorcer.core.context.model.ent.Entry;
import sorcer.core.context.model.par.ParModel;
import sorcer.core.invoker.CmdInvoker;
import sorcer.core.invoker.ServiceInvoker;
import sorcer.service.*;
import sorcer.util.GenericUtil;
import sorcer.util.Sorcer;
import sorcer.util.exec.ExecUtils;

import java.io.File;
import java.io.StringReader;
import java.rmi.RemoteException;
import java.util.Properties;

import static java.lang.Math.pow;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.set;
import static sorcer.co.operator.*;
import static sorcer.co.operator.direction;
import static sorcer.eo.operator.*;
import static sorcer.eo.operator.args;
import static sorcer.eo.operator.value;
import static sorcer.po.operator.*;
import static sorcer.po.operator.set;
import static sorcer.service.Signature.Direction;
import static sorcer.util.exec.ExecUtils.CmdResult;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "rawtypes", "unchecked" } )
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/sml")
public class Entries {
	private final static Logger logger = LoggerFactory.getLogger(Entries.class);


	@Test
	public void directionalEntries() throws Exception {

		Entry x1 = ent("arg/x1", 100.0);
		assertEquals(100.0, value(x1));
        assertTrue(direction(x1) == null);

		Entry x2 = inEnt("arg/x2", 20.0);
		assertEquals(20.0, value(x2));
        assertTrue(direction(x2) == Direction.IN);

		Entry x3 = outEnt("arg/x3", 80.0);
		assertEquals(80.0, value(x3));
        assertTrue(direction(x3) == Direction.OUT);

        // entry of entry
		Entry x4 = inoutEnt("arg/x4", x3);
		assertEquals(80.0, value(x4));
        assertTrue(direction(x4) == Direction.INOUT);
		assertEquals(name(asis(x4)), "arg/x3");
        assertTrue(direction((Entry) asis(x4)) == Direction.OUT);
	}

	@Test
	public void expressionEntry() throws Exception {

		Entry z1 = ent("z1", expr("x1 + 4 * x2 + 30",
					args("x1", "x2"),
					context(ent("x1", 10.0), ent("x2", 20.0))));

		assertEquals(120.0, value(z1));
	}

	@Test
	public void bindingEntryArgs() throws Exception {

		Entry y = ent("y", expr("x1 + x2", args("x1", "x2")));

		assertTrue(value(y, ent("x1", 10.0), ent("x2", 20.0)).equals(30.0));
	}


	public static class Doer implements Invocation<Double> {

        @Override
        public Double invoke(Context cxt, Arg... entries) throws RemoteException, ContextException {
            Entry<Double> x = ent("x", 20.0);
            Entry<Double> y = ent("y", 30.0);
            Entry<Double> z = ent("z", invoker("x - y", x, y));

            if (value(cxt, "x") != null)
                set(x, value(cxt, "x"));
            if (value(cxt, "y") != null)
                set(y, value(cxt, "y"));
            return value(y) + value(x) + value(z);
        }
    };

	@Test
	public void methodEntry() throws Exception {

        Object obj = new Doer();

        // no scope for invocation
        Entry m1 = ent("m1", methodInvoker("invoke", obj));
        assertEquals(value(m1), 40.0);

        // method invocation with a scope
        Context scope = context(ent("x", 200.0), ent("y", 300.0));
        m1 = ent("m1", methodInvoker("invoke", obj, scope));
        assertEquals(value(m1), 400.0);
    }

    @Test
    public void systemCmdInvoker() throws Exception {
        Args args;
        if (GenericUtil.isLinuxOrMac()) {
            args = args("sh",  "-c", "echo $USER");
        } else {
            args = args("cmd",  "/C", "echo %USERNAME%");
        }

        Entry cmd = ent("cmd", invoker(args));

        CmdResult result = (CmdResult) value(cmd);
        logger.info("result: " + result);

        logger.info("result out: " + result.getOut());
        logger.info("result err: " + result.getErr());
        if (result.getExitValue() != 0)
            throw new RuntimeException();
        String userName = (GenericUtil.isWindows() ? System.getenv("USERNAME") : System.getenv("USER"));

        logger.info("User: " + userName);
        logger.info("Got: " + result.getOut());
        assertEquals(userName.toLowerCase(), result.getOut().trim().toLowerCase());
    }

	@Test
	public void lambdaEntries() throws Exception {

		Entry y1 = lambda("y1", () -> 20.0 * pow(0.5, 6) + 10.0);

		assertEquals(10.3125, value(y1));

		Entry y2 = lambda("y2", (Context<Double> cxt) ->
						value(cxt, "x1") + value(cxt, "x2"),
				    context(ent("x1", 10.0), ent("x2", 20.0)));

		assertEquals(30.0, value(y2));
	}

    @Test
    public void signatureEntry() throws Exception {

        Entry y1 = ent("y1", sig("add", AdderImpl.class, result("add/out",
                        inPaths("add/x1", "add/x2"))),
                    context(inEnt("x1", 10.0), inEnt("x2", 20.0)));

        assertEquals(30.0, value(y1));
    }
}
