package org.richfaces.component.focus;

import static org.jboss.arquillian.graphene.Graphene.guardAjax;
import static org.jboss.arquillian.graphene.Graphene.guardHttp;
import static org.jboss.arquillian.graphene.Graphene.waitGui;
import static org.junit.Assert.assertEquals;

import java.net.URL;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.warp.Activity;
import org.jboss.arquillian.warp.Warp;
import org.jboss.arquillian.warp.WarpTest;
import org.jboss.arquillian.warp.jsf.AfterPhase;
import org.jboss.arquillian.warp.jsf.BeforePhase;
import org.jboss.arquillian.warp.jsf.Phase;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.richfaces.component.AbstractFocus;
import org.richfaces.integration.RichDeployment;
import org.richfaces.renderkit.FocusRendererBase;
import org.richfaces.shrinkwrap.descriptor.FaceletAsset;
import org.richfaces.utils.focus.ElementIsFocused;
import org.richfaces.utils.focus.FocusRetriever;

@RunAsClient
@WarpTest
@RunWith(Arquillian.class)
public class ITFocusValidationAware {

    @Drone
    private WebDriver browser;

    @ArquillianResource
    private URL contextPath;

    @FindBy(id = "form:submit")
    private WebElement submitButton;

    @FindBy(id = "form:ajax")
    private WebElement ajaxButton;

    @FindBy(id = "form:input2")
    private WebElement input2;

    @FindBy(id = "form:input3")
    private WebElement input3;

    @Deployment
    public static WebArchive createDeployment() {
        RichDeployment deployment = new RichDeployment(ITFocusValidationAware.class);

        deployment.archive().addClasses(ComponentBean.class, VerifyFocusCandidates.class, AbstractComponentAssertion.class);
        deployment.archive().addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");

        addIndexPage(deployment);

        return deployment.getFinalArchive();
    }

    @Before
    public void openInitialPage() {
        browser.get(contextPath.toExternalForm());
    }

    /**
     * Second input will get focus since it has lower tabindex
     */
    @Test
    public void testValidateMultipleInputsDuringFormSubmission() {

        Warp.initiate(new Activity() {
            public void perform() {
                guardHttp(submitButton).click();
            }
        })
                .inspect(new VerifyFocusCandidates("First input should be focused", "form:input1 form:input2",
                        "form:input1 form:input2"));

        waitGui().until(new ElementIsFocused(input2));
    }

    @Test
    public void testValidateMultipleInputsDuringAjax() {

        Warp.initiate(new Activity() {
            public void perform() {
                guardAjax(ajaxButton).click();
            }
        })
                .inspect(new VerifyFocusCandidates("First input should be focused", "form:input1 form:input2",
                        "form:input1 form:input2"));

        waitGui().until(new ElementIsFocused(input2));
    }

    @Test
    public void testGlobalMessageIsIgnored() {

        Warp.initiate(new Activity() {

            @Override
            public void perform() {
                guardHttp(submitButton).click();
            }
        }).inspect(new AbstractComponentAssertion() {
            private static final long serialVersionUID = 1L;

            @BeforePhase(Phase.RENDER_RESPONSE)
            public void addGlobalMessage() {
                FacesContext context = FacesContext.getCurrentInstance();
                context.addMessage(null, new FacesMessage("global message"));
            }

            @AfterPhase(Phase.RENDER_RESPONSE)
            public void verifyGlobalMessageIsIgnored() {
                FacesContext context = FacesContext.getCurrentInstance();

                AbstractFocus component = bean.getComponent();
                FocusRendererBase renderer = bean.getRenderer();
                String candidates = renderer.getFocusCandidatesAsString(context, component);

                assertEquals("form", candidates);
            }
        });

        waitGui().until(new ElementIsFocused(input3));
    }

    private WebElement getFocusedElement() {
        return FocusRetriever.retrieveActiveElement();
    }

    private static void addIndexPage(RichDeployment deployment) {
        FaceletAsset p = new FaceletAsset();

        p.body("<h:form id='form'>");
        p.body("    <rich:focus id='focus' binding='#{componentBean.component}' />");

        p.body("    <h:inputText id='input1' />");
        p.body("    <h:inputText id='input2' tabindex='2' />");
        p.body("    <h:inputText id='input3' tabindex='1' />");

        p.body("    <h:commandButton id='submit' value='Submit' />");

        p.body("    <a4j:commandButton id='ajax' render='@form' value='Ajax' />");

        p.body("</h:form>");

        deployment.archive().addAsWebResource(p, "index.xhtml");
    }
}
