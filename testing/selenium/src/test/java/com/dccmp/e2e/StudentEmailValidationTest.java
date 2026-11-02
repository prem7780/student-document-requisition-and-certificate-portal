package com.dccmp.e2e;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.nio.file.*;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;

/** This test requires a running DCCMP frontend/backend. No screenshots are pre-generated. */
class StudentEmailValidationTest {
 @Test void wrongEmailIsRejectedAndEvidenceIsCaptured() throws Exception {
  WebDriver driver=new ChromeDriver();
  Path evidence=Path.of("../evidence");Files.createDirectories(evidence);
  try {
   driver.get(System.getProperty("dccmp.url","http://localhost:5173")+"/student");
   driver.findElement(By.cssSelector("input[placeholder='23NG1A0501']")).sendKeys("23NG1A0501");
   WebElement email=driver.findElement(By.cssSelector("input[type=email]"));email.clear();email.sendKeys("wrong.email@usharama.in");
   driver.findElement(By.xpath("//button[contains(.,'Send OTP')]")).click();
   new WebDriverWait(driver,Duration.ofSeconds(12)).until(d->d.getPageSource().contains("do not match"));
   assertTrue(driver.getPageSource().contains("do not match"));
   Files.copy(((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE).toPath(),evidence.resolve("TC-EMAIL-001-invalid-email.png"),StandardCopyOption.REPLACE_EXISTING);
  }finally{driver.quit();}
 }
}
