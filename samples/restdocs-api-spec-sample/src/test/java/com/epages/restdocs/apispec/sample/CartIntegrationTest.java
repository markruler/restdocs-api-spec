package com.epages.restdocs.apispec.sample;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.headerWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.data.rest.webmvc.RestMediaTypes.HAL_JSON;
import static org.springframework.data.rest.webmvc.RestMediaTypes.TEXT_URI_LIST;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.fileUpload;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@AutoConfigureRestDocs
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class CartIntegrationTest extends BaseIntegrationTest {

    String cartId;

    @Test
    public void should_create_cart() throws Exception {

        whenCartIsCreated();

        resultActions
                .andExpect(status().isCreated())
                .andDo(document("carts-create", resource("Create a cart")))
        ;
    }

    @Test
    public void should_add_product_to_cart() throws Exception {
        givenCart();
        givenProduct();

        whenProductIsAddedToCart();

        resultActions
                .andExpect(status().isOk())
                .andDo(document("cart-add-product",
                        resource("Add products to a cart")))
        ;
    }


    @Test
    public void should_add_invoice_to_cart() throws Exception {
        whenCartInvoiceIsCreated();

        resultActions
                .andExpect(status().isOk())
                .andDo(document("cart-add-invoice",
                        resource(
                                ResourceSnippetParameters.builder()
                                        .description("Add invoice to a cart")
                                        .requestHeaders(
                                                headerWithName("Content-Type").description("The content type of the file.")
                                        )
//                                        .pathParameters(
//                                                parameterWithName("id").description("the cart id"))
                                        .build()
                        )))
        ;
    }

    @Test
    public void should_get_cart() throws Exception {
        givenCartWithProduct();

        whenCartIsRetrieved();

        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("products", hasSize(1)))
                .andExpect(jsonPath("products[0].quantity", is(1)))
                .andExpect(jsonPath("products[0].product.name", notNullValue()))
                .andExpect(jsonPath("total", notNullValue()))
                .andDo(document("cart-get",
                        resource(
                                ResourceSnippetParameters.builder()
                                        .description("Get a cart by id")
                                        .pathParameters(
                                                parameterWithName("id").description("the cart id"))
                                        .responseFields(
                                                fieldWithPath("total").description("Total amount of the cart."),
                                                fieldWithPath("products").description("The product line item of the cart."),
                                                subsectionWithPath("products[]._links.product").description("Link to the product."),
                                                fieldWithPath("products[].quantity").description("The quantity of the line item."),
                                                subsectionWithPath("products[].product").description("The product the line item relates to."),
                                                subsectionWithPath("_links").description("Links section."))
                                        .links(
                                                linkWithRel("self").ignored(),
                                                linkWithRel("order").description("Link to order the cart."))
                                        .build()
                        )
                ));
    }

    @Test
    public void should_order_cart() throws Exception {
        givenCartWithProduct();

        whenCartIsOrdered();

        resultActions
                .andExpect(status().isOk())
                .andDo(document("cart-order", resource("Order a cart")))
        ;
    }

    private void whenProductIsAddedToCart() throws Exception {
        resultActions = mockMvc.perform(post("/carts/{id}/products", cartId)
                .contentType(TEXT_URI_LIST)
                .content(entityLinks.linkForSingleResource(Product.class, productId).toUri().toString()));
    }

    private void whenCartIsCreated() throws Exception {
        resultActions = mockMvc.perform(post("/carts"));

        String location = resultActions.andReturn().getResponse().getHeader(LOCATION);
        cartId = location.substring(location.lastIndexOf("/") + 1);
    }

    private void whenCartInvoiceIsCreated() throws Exception {
        MockMultipartFile mockPdf =
                new MockMultipartFile("file", "test.pdf", MediaType.APPLICATION_PDF_VALUE, "test".getBytes());

        resultActions = mockMvc.perform(
                fileUpload("/carts/{id}/invoice", "someId")
                        .file("invoice", mockPdf.getBytes())
        );
    }

    private void whenCartIsRetrieved() throws Exception {
        resultActions = mockMvc.perform(get("/carts/{id}", cartId)
                .accept(HAL_JSON))
                .andDo(print());
    }

    private void whenCartIsOrdered() throws Exception {
        resultActions = mockMvc.perform(post("/carts/{id}/order", cartId));
    }


    private void givenCart() throws Exception {
        whenCartIsCreated();
        resultActions.andExpect(status().isCreated());
    }

    private void givenCartWithProduct() throws Exception {
        givenCart();
        givenProduct();
        whenProductIsAddedToCart();

        resultActions.andExpect(status().isOk());
    }
}
