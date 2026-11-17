package com.idonice.mini_wsa.repository.projection;

import com.idonice.mini_wsa.model.Action;
import com.idonice.mini_wsa.model.Category;

// One cell of the category x action table. Internal: never sent as JSON.
public record CategoryActionCount(
        Category category,
        Action action,
        Long count,
        Long scoreSum
) {}
