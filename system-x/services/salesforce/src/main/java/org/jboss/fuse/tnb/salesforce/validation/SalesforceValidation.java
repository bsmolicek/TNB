package org.jboss.fuse.tnb.salesforce.validation;

import org.jboss.fuse.tnb.salesforce.account.SalesforceAccount;
import org.jboss.fuse.tnb.salesforce.dto.Account;
import org.jboss.fuse.tnb.salesforce.dto.Case;
import org.jboss.fuse.tnb.salesforce.dto.Lead;

import org.junit.jupiter.api.Assertions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.force.api.ForceApi;
import com.force.api.QueryResult;

import java.util.Optional;

public class SalesforceValidation {
    private static final Logger LOG = LoggerFactory.getLogger(SalesforceValidation.class);

    private ForceApi client;
    private SalesforceAccount account;

    public SalesforceValidation(ForceApi client, SalesforceAccount account) {
        this.client = client;
        this.account = account;
    }

    public void createNewLead(String firstName, String lastName, String email, String companyName) {
        final Lead lead = new Lead(firstName, lastName, email, companyName);
        String leadId = client.createSObject("lead", lead);
        LOG.debug("Created lead with id " + leadId);
    }

    public void updateLead(String email, Lead newLead) {
        Optional<Lead> sfLead = getLeadByEmail(email);
        Assertions.assertTrue(sfLead.isPresent());
        String leadId = sfLead.get().getId();
        client.updateSObject("lead", leadId, newLead);
    }

    public void deleteLead(String email) {
        final Optional<Lead> lead = getLeadByEmail(email);
        if (lead.isPresent()) {
            String leadId = lead.get().getId();
            client.deleteSObject("lead", leadId);
            LOG.debug("Deleting salesforce lead: {}", lead.get());
        }
    }

    public String createCase(String accountId, String status, String origin, String subject) {
        final Case newCase = new Case(accountId, status, origin, subject);
        String caseId = client.createSObject("Case", newCase);
        LOG.debug("Created case with id " + caseId);
        return caseId;
    }

    public void deleteCase(String id) {
        client.deleteSObject("case", id);
        LOG.debug("Deleting salesforce case with id : {}", id);
    }

    public String createAccount(String name, String phone) {
        final Account sfAccount = new Account(name, phone);
        String accountId = client.createSObject("Account", sfAccount);
        LOG.debug("Created account with id " + accountId);
        return accountId;
    }

    public void deleteAccount(String id) {
        client.deleteSObject("account", id);
        LOG.debug("Deleting salesforce account with id : {}", id);
    }

    public Optional<Lead> getLeadByEmail(String emailAddress) {
        final QueryResult<Lead> queryResult =
            client.query("SELECT Id,FirstName,LastName,Email,Company FROM lead where Email = '"
                + emailAddress + "'", Lead.class
            );
        return queryResult.getTotalSize() > 0 ? Optional.of(queryResult.getRecords().get(0)) : Optional.empty();
    }
}