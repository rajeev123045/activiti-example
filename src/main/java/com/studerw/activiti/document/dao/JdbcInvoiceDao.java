package com.studerw.activiti.document.dao;

import com.google.common.collect.ImmutableMap;
import com.studerw.activiti.document.InvoiceRowMapper;
import com.studerw.activiti.model.Invoice;
import com.studerw.activiti.web.PagingCriteria;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Types;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Implementation of {@link BookReportDao} using Spring / JDBC
 *
 * @author studerw
 */

@Component("invoiceDao")
@Repository
public class JdbcInvoiceDao implements InvoiceDao {
    private static final Logger log = LoggerFactory.getLogger(JdbcInvoiceDao.class);
    protected DataSource ds;
    protected NamedParameterJdbcTemplate namedJdbcTemplate;

    public DataSource getDataSource() {
        return this.ds;
    }

    @Autowired
    @Qualifier("dataSource")
    public void setDataSource(DataSource datasource) {
        this.ds = datasource;
        this.namedJdbcTemplate = new NamedParameterJdbcTemplate(this.ds);
    }

    @Override
    @Transactional(readOnly = true)
    public int getCount() {
        String sql = "SELECT count(*) FROM INVOICE";
        @SuppressWarnings("unchecked")
        int count = this.namedJdbcTemplate.queryForObject(sql, Collections.EMPTY_MAP, Integer.class);
        log.debug("Got count: " + count + " of book reports");
        return count;
    }

    @Override public List<Invoice> readAll() {
        String sql = "SELECT * FROM INVOICE ORDER BY created_date ASC";
        List<Invoice> reports = this.namedJdbcTemplate.query(sql, new InvoiceRowMapper());
        log.debug("got all book reports: {}", reports.size());
        return reports;
    }


    @Override public List<Invoice> readPage(PagingCriteria criteria) {
        log.debug("reading page with criteria: " + criteria);
        if (criteria == null || criteria.getLimit() == null || criteria.getStart() == null) {
            log.warn("criteria invalid - reading all instead of subset");
            return readAll();
        }
        String sql = "SELECT LIMIT :start :limit * FROM INVOICE ORDER BY created_date ASC";
        BeanPropertySqlParameterSource source = new BeanPropertySqlParameterSource(criteria);

        List<Invoice> documents = this.namedJdbcTemplate.query(sql, source, new InvoiceRowMapper());
        log.debug(documents.size() + " Documents returned using criteria: " + criteria);

        return documents;

    }

    @Override public String create(Invoice obj) {
        log.debug("Inserting Document into SQL backend: " + obj);
        checkArgument(StringUtils.isBlank(obj.getId()), "Document id cannot be already set");

        String id = UUID.randomUUID().toString();
        obj.setId(id);
        String sql = "INSERT INTO INVOICE (id, author, payee, amount, group_id, doc_type, doc_state, created_date, title) " +
                "VALUES (:id, :author, :payee, :amount, :groupId, :docType, :docState, :createdDate, :title)";

        BeanPropertySqlParameterSource source = new BeanPropertySqlParameterSource(obj);
        source.registerSqlType("docState", Types.VARCHAR);
        source.registerSqlType("docType", Types.VARCHAR);
        int results = this.namedJdbcTemplate.update(sql, source);
        log.debug("Got: " + results + " results");
        obj.setId(id);
        return id;
    }

    @Override public void createWithId(Invoice obj) {
        throw new UnsupportedOperationException("not supported");
    }

    @Override public void update(Invoice obj) {
        checkArgument(StringUtils.isNotBlank(obj.getId()), "document id cannot be blank");
        String sql = "UPDATE INVOICE SET author=:author, payee=:payee, amount=:amount" +
                " group_Id=:groupId, doc_state=:docState, title=:title WHERE id=:id";

        BeanPropertySqlParameterSource source = new BeanPropertySqlParameterSource(obj);
        source.registerSqlType("docState", Types.VARCHAR);
        int results = this.namedJdbcTemplate.update(sql, source);
        log.debug("Updated: {} Invoices", results);
    }

    @Override public void delete(String id) {
        String sql = "DELETE FROM INVOICE WHERE id = :id";
        Map<String, String> params = ImmutableMap.of("id", id);
        int deleted = this.namedJdbcTemplate.update(sql, params);
        log.debug("Deleted: {} Invoices", deleted);
    }

    @Override public Invoice read(String id) {
        String sql = "SELECT * FROM INVOICE where id = :id";
        Map<String, String> params = ImmutableMap.of("id", id);
        Invoice Invoice = this.namedJdbcTemplate.queryForObject(sql, params, new InvoiceRowMapper());
        return Invoice;
    }
}
