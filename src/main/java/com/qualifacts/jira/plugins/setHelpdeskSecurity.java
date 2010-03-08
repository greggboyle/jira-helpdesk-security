package com.qualifacts.jira.plugins;

import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.ModifiedValue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.ManagerFactory;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.security.IssueSecurityLevelManager;
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder;
import com.atlassian.jira.issue.util.IssueChangeHolder;

import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.FunctionProvider;
import com.opensymphony.user.User;

import org.ofbiz.core.entity.GenericValue;

import java.util.Collection;
import java.util.Properties;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.io.FileInputStream;

public class setHelpdeskSecurity implements FunctionProvider {

	private final IssueSecurityLevelManager issueSecurityLevelManager = ManagerFactory.getIssueSecurityLevelManager(); 

	public void execute(Map transientVars, Map args, PropertySet ps) {
                Properties props = new Properties();
		MutableIssue issue = (MutableIssue) transientVars.get("issue");
		User reporter = issue.getReporter();
		List reporterGroups = reporter.getGroups();
		String logPrefix = new String(); 
		int success = 0;

		System.out.println(logPrefix+" Attempting Updates");
		try {
			props.load(new FileInputStream("jiraProject.properties"));
                        logPrefix = props.getProperty("log.message.prefix")+issue.getKey();
			//Update Reporting Customer Field 
			IssueManager issueManager = ComponentManager.getInstance().getIssueManager();
			CustomFieldManager cfManager = ComponentManager.getInstance().getCustomFieldManager(); 
			CustomField cf = cfManager.getCustomFieldObjectByName(props.getProperty("account.custom.field"));
			IssueChangeHolder changeHolder = new DefaultIssueChangeHolder();
			
			Collection issueSecurityLevels = issueSecurityLevelManager.getUsersSecurityLevels(issue.getProject(), reporter);
			
			/** Set Issue Level Security */
			groups:
			for (Iterator iterator = reporterGroups.iterator(); iterator.hasNext();) {
				
				String groupName = (String) iterator.next();
			
				for (Iterator iterator1 = issueSecurityLevels.iterator(); iterator1.hasNext();) {
					
					GenericValue securityLevel = (GenericValue) iterator1.next();
					String s = securityLevel.getString("name");

					//if (s.equals("Reporter and QSI") && groupName.startsWith("SG-")) {
					if (s.startsWith(props.getProperty("security.level.prefix")) && groupName.startsWith(props.getProperty("account.group.prefix"))) {
						cf.updateValue(null,issue,new ModifiedValue(issue.getCustomFieldValue(cf),groupName.subSequence(3, groupName.length())),changeHolder);
						System.out.println(logPrefix+" Account Applied:"+groupName.subSequence(3, groupName.length()));
						issue.setSecurityLevel(securityLevel);
						issue.store();
						System.out.println(logPrefix+" Applied Level:"+s);
						success=1;
						break groups;
					}
				}
			}
			
			if (success==0) {
				System.out.println(logPrefix+"WARNING: Issue Account and Issue Security Level Not Applied");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return;
		
	}

}
