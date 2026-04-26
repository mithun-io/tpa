package com.tpa.mapper;

import com.tpa.dto.request.CustomerRequest;
import com.tpa.dto.response.CustomerResponse;
import com.tpa.entity.Customer;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    Customer toCustomer(CustomerRequest customerRequest);

    CustomerResponse toCustomerResponse(Customer customer);

    List<CustomerResponse> toCustomerResponses(List<Customer> customers);
}