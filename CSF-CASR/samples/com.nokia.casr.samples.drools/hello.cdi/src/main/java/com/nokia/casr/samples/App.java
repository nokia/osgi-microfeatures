package com.nokia.casr.samples;

import javax.annotation.PostConstruct;

import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.ops4j.pax.cdi.api.Component;
import org.ops4j.pax.cdi.api.Immediate;

/**
 * Hello world!
 */
@Component
@Immediate
public class App {

	private KieSession ksession;

	public App() {
		System.out.println("Constructor App");
	}

	@PostConstruct
	public void activate() {

		KieServices ks = KieServices.Factory.get();
		KieContainer kcont = ks.newKieClasspathContainer(getClass().getClassLoader());
		KieBase kbase = kcont.getKieBase("sampleKBase");

		this.ksession = kbase.newKieSession();
		System.out.println("KieSession created.");

		for (int i = 0; i < 20; i++) {
			// go !
			Account account = new Account(200);
			account.withdraw(150);
			ksession.insert(account);
			ksession.fireAllRules();
		}
	}
}
