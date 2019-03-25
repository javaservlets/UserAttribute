/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2018 ForgeRock AS.
 */


package com.example.forgerock;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.*;
import org.forgerock.openam.core.CoreWrapper;

import javax.inject.Inject;

/**
 * A node that checks to see if zero-page login headers have specified username and shared key
 * for this request.
 */
@Node.Metadata(outcomeProvider = AbstractDecisionNode.OutcomeProvider.class,
        configClass = UserAttribute.Config.class)
public class UserAttribute extends AbstractDecisionNode {

    private static final String BUNDLE = "com/example/forgerock";
    JsonValue context_json;
    private final Config config;
    private final CoreWrapper coreWrapper;
    private final static String DEBUG_FILE = "userAttributeNode";
    protected Debug debug = Debug.getInstance(DEBUG_FILE);


    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        context_json = context.sharedState.copy();
        String usr = context_json.get("username").toString();
        String usr_attr = getUserAttr(usr);
        if (usr_attr != "") { // there is a match, so return true
            log("true / has UserAttribute: " + usr_attr);
            return goTo(true).replaceSharedState(context.sharedState
                    .add("UserAttribute", usr_attr))
                    .build();

        } else {
            log("false / has NO attr"); // no match, so fail
        }
        return goTo(false).build();
    }

    public String getUserAttr(String usr) {
        Reader user_reader = new Reader(usr, config.serverAddress()); // this will query 2 c if usr has a value stored as an attr
        String user_attr = user_reader.getAttributes(config.attributeName()); //
        log("       //     getting att: " + user_attr + "@" + config.serverAddress() + "::" + config.attributeName());

        if (user_attr != null) // otherwise usr <> enrolled
            return user_attr;
        else
            return "";
    }

    public UserAttribute() {
        this.config = new Config() {
            @Override
            public String toString() {
                return super.toString();
            }
        };
        this.coreWrapper = new CoreWrapper();
    }

    public interface Config {

        @Attribute(order = 100)
        default String serverAddress() {
            return "Server Address";
            //"http://robbie.freng.org:8080";
        }

        @Attribute(order = 200)
        default String attributeName() {
            return "Attribute Name";
            //sunIdentityMSISDNNumber
        }

     }

    @Inject
    public UserAttribute(@Assisted Config config, CoreWrapper coreWrapper) throws NodeProcessException {
        this.config = config;
        this.coreWrapper = coreWrapper;
    }

    public void log(String str) {
        debug.message("\r\n           msg:" + str + "\r\n");
        //System.out.println("\n" + str);
    }


}