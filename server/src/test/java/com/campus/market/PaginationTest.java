package com.campus.market;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:pagination-test;MODE=MySQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class PaginationTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;

    private JsonNode page(String path, int number, int size, String... filters) throws Exception {
        MockHttpServletRequestBuilder request = get(path).header("X-User-Id", "1")
                .header("X-User-Role", path.startsWith("/api/admin") ? "ADMIN" : "USER")
                .param("page", String.valueOf(number)).param("size", String.valueOf(size));
        for (int index = 0; index < filters.length; index += 2)
            request.param(filters[index], filters[index + 1]);
        return json.readTree(mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString()).path("data");
    }

    @Test
    void allListsReturnBoundedDistinctPagesAndAccurateTotals() throws Exception {
        String[] paths = { "/api/goods", "/api/goods/mine", "/api/goods/favorites", "/api/orders/mine",
                "/api/wants", "/api/messages/mine", "/api/messages/goods/7", "/api/admin/goods",
                "/api/admin/users", "/api/admin/orders", "/api/admin/messages", "/api/admin/wants",
                "/api/admin/reports" };
        for (String path : paths) {
            JsonNode first = page(path, 1, 5);
            long total = first.path("total").asLong();
            assertTrue(total > 9, path + " needs enough demo data");
            assertEquals(5, first.path("records").size(), path);
            Set<Long> ids = new HashSet<>();
            long seen = 0;
            int last = (int) ((total + 4) / 5);
            for (int number = 1; number <= last; number++) {
                JsonNode data = page(path, number, 5);
                assertEquals(total, data.path("total").asLong(), path);
                assertEquals(number, data.path("page").asInt(), path);
                assertEquals(5, data.path("size").asInt(), path);
                assertTrue(data.path("records").size() <= 5, path);
                for (JsonNode row : data.path("records")) {
                    long id = row.has("id") ? row.path("id").asLong() : row.path("ID").asLong();
                    assertTrue(ids.add(id), path + " duplicate id " + id);
                    seen++;
                }
            }
            assertEquals(total, seen, path);
            assertEquals(0, page(path, last + 1, 5).path("records").size(), path);
        }
    }

    @Test
    void filtersApplyBeforePaginationAndSortingIsStable() throws Exception {
        JsonNode data = page("/api/goods", 1, 5, "category", "教材书籍", "minPrice", "20", "maxPrice", "40",
                "sort", "priceAsc");
        assertTrue(data.path("total").asLong() > 5);
        double previous = 0;
        for (int number = 1; number <= (data.path("total").asLong() + 4) / 5; number++) {
            for (JsonNode row : page("/api/goods", number, 5, "category", "教材书籍", "minPrice", "20",
                    "maxPrice", "40", "sort", "priceAsc").path("records")) {
                assertEquals("教材书籍", row.path("category").asText());
                double price = row.path("price").asDouble();
                assertTrue(price >= 20 && price <= 40 && price >= previous);
                previous = price;
            }
        }
        JsonNode own = page("/api/wants", 1, 5, "onlyMine", "true");
        assertTrue(own.path("total").asLong() > 5);
        for (JsonNode row : own.path("records"))
            assertEquals(1, row.path("userId").asInt());
        for (JsonNode row : page("/api/admin/reports", 1, 5, "status", "RESOLVED").path("records"))
            assertEquals("RESOLVED", row.path("STATUS").asText());
        assertEquals(0, page("/api/goods", 1, 5, "keyword", "没有匹配案例XYZ").path("total").asLong());
        assertTrue(page("/api/goods/mine", 1, 5, "status", "OFF_SHELF").path("total").asLong() > 0);
    }

    @Test
    void invalidPageSizeSortAndPriceRangeAreRejected() throws Exception {
        String[][] invalid = { { "page", "0" }, { "page", "-1" }, { "size", "0" }, { "size", "101" },
                { "sort", "price desc;drop table goods" } };
        for (String[] filter : invalid)
            mvc.perform(get("/api/goods").param(filter[0], filter[1])).andExpect(status().isBadRequest());
        mvc.perform(get("/api/goods").param("minPrice", "100").param("maxPrice", "20"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/admin/goods").header("X-User-Role", "USER")).andExpect(status().isForbidden());
    }
}
