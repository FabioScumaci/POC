/**
 * Copyright (c) Raja Dilip Chowdary Kolli. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.poc.restfulpoc.controller;

import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.poc.restfulpoc.AbstractRestFulPOCApplicationTest;
import com.poc.restfulpoc.data.DataBuilder;
import com.poc.restfulpoc.entities.Address;
import com.poc.restfulpoc.entities.Customer;
import com.poc.restfulpoc.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CustomerController Tests.
 *
 * @author Raja Kolli
 *
 */
public class CustomerControllerITTest extends AbstractRestFulPOCApplicationTest {

	@Autowired
	private CustomerRepository customerRepository;

	@Autowired
	private DataBuilder dataBuilder;

	@Autowired
	private CacheManager cacheManager;

	private String base;

	@BeforeEach
	public void setUp() throws Exception {
		this.base = "/rest/customers/";
		this.customerRepository.deleteAll();
		this.dataBuilder.run();
	}

	@Test
	public void testGetCustomerById() throws Exception {
		final Long customerId = getCustomerIdByFirstName("Raja");
		final ResponseEntity<Customer> response = userRestTemplate().getForEntity(
				String.format("%s%s", this.base, customerId), Customer.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getHeaders().getContentType().toString())
				.isEqualTo(MediaType.APPLICATION_JSON_UTF8_VALUE);

		final Customer customer = response.getBody();

		assertThat(customer.getFirstName()).isEqualTo("Raja");
		assertThat(customer.getLastName()).isEqualTo("Kolli");
		assertThat(customer.getDateOfBirth()).hasDayOfMonth(10).hasMonth(1).hasYear(1982);
		assertThat(customer.getAddress().getStreet()).isEqualTo("High Street");
		assertThat(customer.getAddress().getTown()).isEqualTo("Belfast");
		assertThat(customer.getAddress().getCounty()).isEqualTo("India");
		assertThat(customer.getAddress().getPostcode()).isEqualTo("BT893PY");
	}

	@Test
	public void testGetCustomerByNullId() throws Exception {
		final ResponseEntity<String> response = userRestTemplate()
				.getForEntity(String.format("%s/%s", this.base, null), String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	@DisplayName("Test with Id that doesn't exist")
	public void testGetCustomerByIdWhichDoesntExist() throws Exception {
		final ResponseEntity<Customer> response = userRestTemplate().getForEntity(
				String.format("%s/%s", this.base, Long.MAX_VALUE), Customer.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	public void testGetAllCustomers() throws Exception {
		final ResponseEntity<String> response = userRestTemplate().getForEntity(this.base,
				String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		final List<Customer> customers = convertJsonToCustomers(response.getBody());
		assertThat(customers.size()).isEqualTo(3);
	}

	@Test
	@DisplayName("Creating Customer")
	public void testCreateCustomer() throws Exception {
		final Customer customer = Customer.builder().firstName("Gary").lastName("Steale")
				.dateOfBirth(Date.from(LocalDate.of(1984, Month.MARCH, 8)
						.atStartOfDay(ZoneId.of("UTC")).toInstant()))
				.build();
		customer.setAddress(Address.builder().street("Main Street").town("Portadown")
				.county("Armagh").postcode("BT359JK").build());
		ResponseEntity<Customer> response = userRestTemplate().postForEntity(this.base,
				customer, Customer.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getHeaders().getContentLength()).isEqualTo(0);
		String location = response.getHeaders().getFirst("Location");
		assertThat(location).contains(this.base);

		response = userRestTemplate().getForEntity(location, Customer.class);
		Customer returnedCustomer = response.getBody();
		assertThat(customer.getFirstName()).isEqualTo(returnedCustomer.getFirstName());
		assertThat(customer.getLastName()).isEqualTo(returnedCustomer.getLastName());
		assertThat(customer.getDateOfBirth())
				.isEqualTo(returnedCustomer.getDateOfBirth());
		assertThat(customer.getAddress().getStreet())
				.isEqualTo(returnedCustomer.getAddress().getStreet());
		assertThat(customer.getAddress().getTown())
				.isEqualTo(returnedCustomer.getAddress().getTown());
		assertThat(customer.getAddress().getCounty())
				.isEqualTo(returnedCustomer.getAddress().getCounty());
		assertThat(customer.getAddress().getPostcode())
				.isEqualTo(returnedCustomer.getAddress().getPostcode());

		Customer newCustomer = Customer.builder().firstName("Andy").lastName("Steale")
				.build();
		newCustomer.setAddress(Address.builder().street("Main Street").town("Portadown")
				.county("Armagh").postcode("BT359JK").build());
		response = userRestTemplate().postForEntity(this.base, newCustomer,
				Customer.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getHeaders().getContentLength()).isEqualTo(0);

		location = response.getHeaders().getFirst("Location");
		assertThat(location).contains(this.base);

		response = userRestTemplate().getForEntity(location, Customer.class);
		returnedCustomer = response.getBody();
		assertThat(returnedCustomer.getDateOfBirth()).isNull();

		newCustomer = Customer.builder().firstName("Gary").lastName("Steale")
				.address(Address.builder().street("Main Street").town("Portadown")
						.county("Armagh").postcode("BT359JK").build())
				.build();
		response = userRestTemplate().postForEntity(this.base, newCustomer,
				Customer.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
	}

	@Test
	@DisplayName("Tests InValid Customer")
	public void testInValidCustomer() {
		Customer newCustomer = Customer.builder().firstName(" ").lastName("Steale")
				.dateOfBirth(new Date(new Date().getTime() + TimeUnit.DAYS.toMillis(100)))
				.build();
		ResponseEntity<Customer> response = userRestTemplate().postForEntity(this.base,
				newCustomer, Customer.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
		newCustomer = Customer.builder().firstName(null).lastName("Steale").build();
		response = userRestTemplate().postForEntity(this.base, newCustomer,
				Customer.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	public void testUpdateCustomer() throws Exception {
		final Long customerId = getCustomerIdByFirstName("Raja");
		final ResponseEntity<Customer> getCustomerResponse = userRestTemplate()
				.getForEntity(String.format("%s/%s", this.base, customerId),
						Customer.class);
		assertThat(getCustomerResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(getCustomerResponse.getHeaders().getContentType().toString())
				.isEqualTo(MediaType.APPLICATION_JSON_UTF8_VALUE);

		final Customer persistedCustomer = getCustomerResponse.getBody();
		assertThat(persistedCustomer.getFirstName()).isEqualTo("Raja");
		assertThat(persistedCustomer.getLastName()).isEqualTo("Kolli");
		assertThat(persistedCustomer.getDateOfBirth()).hasDayOfMonth(10).hasMonth(1)
				.hasYear(1982);
		assertThat(persistedCustomer.getAddress().getStreet()).isEqualTo("High Street");
		assertThat(persistedCustomer.getAddress().getTown()).isEqualTo("Belfast");
		assertThat(persistedCustomer.getAddress().getCounty()).isEqualTo("India");
		assertThat(persistedCustomer.getAddress().getPostcode()).isEqualTo("BT893PY");

		persistedCustomer.setFirstName("Wayne");
		persistedCustomer.setLastName("Rooney");

		/* PUT updated customer */
		final HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		final HttpEntity<Customer> entity = new HttpEntity<Customer>(persistedCustomer,
				headers);
		final ResponseEntity<Customer> response = userRestTemplate().exchange(
				String.format("%s/%s", this.base, customerId), HttpMethod.PUT, entity,
				Customer.class, customerId);

		assertThat(response.getBody()).isNotNull();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		final Customer updatedCustomer = response.getBody();
		assertThat(updatedCustomer.getFirstName()).isEqualTo("Wayne");
		assertThat(updatedCustomer.getLastName()).isEqualTo("Rooney");
		assertThat(updatedCustomer.getDateOfBirth()).hasDayOfMonth(10).hasMonth(1)
				.hasYear(1982);
		assertThat(updatedCustomer.getAddress().getStreet()).isEqualTo("High Street");
		assertThat(updatedCustomer.getAddress().getTown()).isEqualTo("Belfast");
		assertThat(updatedCustomer.getAddress().getCounty()).isEqualTo("India");
		assertThat(updatedCustomer.getAddress().getPostcode()).isEqualTo("BT893PY");
	}

	@Test
	public void testUpdateCustomerInValid() throws Exception {
		final ResponseEntity<Customer> getCustomerResponse = userRestTemplate()
				.getForEntity(String.format("%s/%s", this.base, 999), Customer.class);
		assertThat(getCustomerResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	public void testRemoveInValidCustomer() throws Exception {
		final ResponseEntity<Customer> response = userRestTemplate()
				.getForEntity(String.format("%s/%s", this.base, 999), Customer.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	public void testRemoveCustomer() throws Exception {
		final Long customerId = getCustomerIdByFirstName("Raja");
		final ResponseEntity<Customer> response = userRestTemplate().getForEntity(
				String.format("%s/%s", this.base, customerId), Customer.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getHeaders().getContentType().toString())
				.isEqualTo(MediaType.APPLICATION_JSON_UTF8_VALUE);

		final Customer customer = response.getBody();
		assertThat(customer.getFirstName()).isEqualTo("Raja");
		assertThat(customer.getLastName()).isEqualTo("Kolli");
		assertThat(customer.getDateOfBirth()).hasDayOfMonth(10).hasMonth(1).hasYear(1982);
		assertThat(customer.getAddress().getStreet()).isEqualTo("High Street");
		assertThat(customer.getAddress().getTown()).isEqualTo("Belfast");
		assertThat(customer.getAddress().getCounty()).isEqualTo("India");
		assertThat(customer.getAddress().getPostcode()).isEqualTo("BT893PY");

		/* delete customer */
		adminRestTemplate().delete(String.format("%s/%s", this.base, customerId),
				Customer.class);

		// Sleeping for 1 second so that JMS message is consumed
		try {
			TimeUnit.SECONDS.sleep(1);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}

		/* attempt to get customer and ensure we get a 404 */
		final ResponseEntity<Customer> secondCallResponse = userRestTemplate()
				.getForEntity(String.format("%s/%s", this.base, customerId),
						Customer.class);
		assertThat(secondCallResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	public void testDeleteAllCustomers() throws Exception {
		/* delete customer */
		adminRestTemplate().delete(this.base);

		final ResponseEntity<Customer> response = userRestTemplate()
				.getForEntity(this.base, Customer.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
	}

	@Test
	public void validateCache() {
		final Cache customersCache = this.cacheManager.getCache("customer");
		assertThat(customersCache).isNotNull();
		customersCache.clear(); // Simple test assuming the cache is empty
		final Long custId = getCustomerIdByFirstName("Raja");
		assertThat(customersCache.get(custId)).isNull();
		final ResponseEntity<Customer> response = userRestTemplate()
				.getForEntity(String.format("%s/%s", this.base, custId), Customer.class);
		final Customer customer = response.getBody();
		final Customer cacheCustomer = (Customer) customersCache.get(custId).get();
		assertThat(cacheCustomer.getFirstName()).isEqualTo(customer.getFirstName());
		assertThat(cacheCustomer.getLastName()).isEqualTo(customer.getLastName());
		assertThat(cacheCustomer.getDateOfBirth()).isEqualTo(customer.getDateOfBirth());
		assertThat(cacheCustomer.getAddress().getCounty())
				.isEqualTo(customer.getAddress().getCounty());
	}

	/**
	 * Convenience method for testing that gives us the customer id based on test
	 * customers name. Need this as IDs will increment as tests are rerun
	 * @param firstName
	 * @return customer Id
	 */
	private Long getCustomerIdByFirstName(String firstName) {
		return this.customerRepository.findByFirstName(firstName).stream().findAny().get()
				.getId();
	}

	private List<Customer> convertJsonToCustomers(String json) throws Exception {
		final ObjectMapper mapper = new ObjectMapper();
		return mapper.readValue(json, TypeFactory.defaultInstance()
				.constructCollectionType(List.class, Customer.class));
	}

}