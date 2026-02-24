package io.github.gmathi.novellibrary.cleaner

import org.jsoup.nodes.Document


class CloudFlareDDoSTagCleaner : HtmlCleaner() {

    override fun additionalProcessing(doc: Document) {
        // Find Cloudflare DDoS protection elements
        val cloudflareElements = doc.body().select("a[href*='cloudflare.com']")
            .filter { it.text().contains("DDoS protection by Cloudflare", ignoreCase = true) }
        
        cloudflareElements.forEach { element ->
            // Add retry link with better styling and messaging
            element.append("""
                <br/><br/>
                <div style="padding: 15px; background-color: #f8f9fa; border-left: 4px solid #007bff; margin: 10px 0;">
                    <p style="margin: 0 0 10px 0; font-weight: bold;">Cloudflare Protection Detected</p>
                    <p style="margin: 0 0 10px 0;">This page is protected by Cloudflare. Click the button below to bypass the protection.</p>
                    <a href='abc://retry_internal' style="display: inline-block; padding: 10px 20px; background-color: #007bff; color: white; text-decoration: none; border-radius: 4px;">
                        Retry with Cloudflare Bypass
                    </a>
                </div>
            """.trimIndent())
        }
        
        // Also check for Cloudflare challenge pages by looking for specific patterns
        val challengeElements = doc.select("div[id*='cf'], div[class*='cf-'], div[class*='cloudflare']")
        if (challengeElements.isNotEmpty() && doc.select("title:contains(Just a moment)").isNotEmpty()) {
            doc.body().prepend("""
                <div style="padding: 20px; background-color: #fff3cd; border: 1px solid #ffc107; margin: 20px; border-radius: 4px;">
                    <h3 style="margin-top: 0; color: #856404;">Cloudflare Challenge Detected</h3>
                    <p>This page requires Cloudflare verification. The app will automatically attempt to bypass this protection.</p>
                    <a href='abc://retry_internal' style="display: inline-block; padding: 10px 20px; background-color: #ffc107; color: #212529; text-decoration: none; border-radius: 4px; margin-top: 10px;">
                        Retry Now
                    </a>
                </div>
            """.trimIndent())
        }
    }

}
