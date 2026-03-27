package com.duster.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class AdminWebController {

    @GetMapping("/admin")
    fun adminHome(): String = "redirect:/admin/clients.html"
}
