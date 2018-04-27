package test.java.basics;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxBinary;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxDriverLogLevel;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.Select;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class SubmissionTest {
	
	WebDriver driver;
	Properties properties;
	String startPage = "https://dev.inge.mpdl.mpg.de/pubman/faces/HomePage.jsp";
	
	String expectedName = "Test1 Moderator_Depositor";
	
	@BeforeClass
	public void setup() throws FileNotFoundException {
		setupDriver();
		setupProperties();
	}
	
	private void setupDriver() {
		System.setProperty("webdriver.gecko.driver", "/" + System.getenv("geckodriver"));
		FirefoxOptions options = new FirefoxOptions();
		options.setCapability("marionette", true);
		FirefoxBinary binary = new FirefoxBinary();
		options.setBinary(binary);
		options.setLogLevel(FirefoxDriverLogLevel.TRACE);
		driver = new FirefoxDriver(options);
		driver.manage().window().setSize(new Dimension(1024, 768));
		driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
		driver.navigate().to(startPage);
	}
	
	private void setupProperties() throws FileNotFoundException {
		String propertiesEnvName = "/" + System.getenv("pubmanTestData");
		properties = new Properties();
		FileInputStream input = new FileInputStream(new File(propertiesEnvName));

		try {	
			properties.load(input);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
        	if (input != null) { try { input.close(); } catch (IOException e) { e.printStackTrace(); } }
        }
	}
	
	@Test(priority = 1)
	public void login() {
		WebElement usernameBox = driver.findElement(By.xpath("//input[contains(@id, 'inputUsername')]"));
		usernameBox.sendKeys(properties.getProperty("modDepUsername1"));
		WebElement passwordBox = driver.findElement(By.xpath("//input[contains(@id, 'inputSecretPassword')]"));
		passwordBox.sendKeys(properties.getProperty("modDepPassword1"));
		WebElement loginButton = driver.findElement(By.xpath("//input[contains(@id, 'lnkLogin')]"));
		loginButton.click();
		
		String actualName = driver.findElement(By.xpath("//a[contains(@id, 'lnkAccountUserName')]")).getText();
		Assert.assertEquals(actualName, expectedName, "Expected and actual name don't match.");
	}
	
	@Test(priority = 2)
	public void viewSubmission() {
		driver.findElement(By.id("Header:lnkSubmission")).click();
		String[] subsections = {"EASY", "FULL", "FETCH", "IMPORT"};
		for (String subsection : subsections) {
			try {
				driver.findElement(By.linkText(subsection));
			}
			catch (NoSuchElementException exc) {
				Assert.fail("No such section is displayed: " + subsection);
			}
		}
	}

	@Test(priority = 3)
	public void createJournalArticle() {
		driver.navigate().to(startPage);
		driver.findElement(By.id("Header:lnkSubmission")).click();
		driver.findElement(By.linkText("FULL")).click();
		
		WebElement titleBox = driver.findElement(By.id("form1:inputTitleText"));
		titleBox.sendKeys("Test item for Jenkins integration: " + Calendar.getInstance().getTimeInMillis());
		
		WebElement familyNameBox = driver.findElement(By.xpath("//input[contains(@id, ':0:inpcreator_persons_person_family_name_optional')]"));
		familyNameBox.sendKeys("Testermann");
		WebElement organisationNrBox = driver.findElement(By.xpath("//input[contains(@id, ':0:inppersons_person_ous_optional')]"));
		organisationNrBox.sendKeys("1");
		WebElement organisationNameBox = driver.findElement(By.xpath("//textarea[contains(@id, ':0:inporganizations_organization_name')]"));
		organisationNameBox.sendKeys("MPDL");
		
		WebElement datePrintBox = driver.findElement(By.id("form1:txtDatePublishedInPrint"));
		datePrintBox.sendKeys("2017-11-08");
		
		WebElement sourceGenreDropbox = driver.findElement(By.xpath("//select[contains(@id, ':0:selChooseSourceGenre')]"));
		Select genreSelect = new Select(sourceGenreDropbox);
		genreSelect.selectByValue("BOOK");
		WebElement sourceTitleBox = driver.findElement(By.xpath("//textarea[contains(@id, ':0:inpSourceTitle_Journal')]"));
		sourceTitleBox.sendKeys("Test Book");
		
		WebElement submitButton = driver.findElement(By.id("form1:lnkSaveAndSubmit"));
		submitButton.click();
		WebElement submitCommentBox = driver.findElement(By.tagName("textarea"));
		submitCommentBox.sendKeys("Test");
		
		WebElement submitArticle = driver.findElement(By.xpath("//a[contains(@id, 'lnkSave')]"));
		submitArticle.click();
	}
	
	@Test(priority = 4)
	public void simpleSearch() {
		driver.navigate().to(startPage);
		WebElement searchBox = driver.findElement(By.xpath("//input[contains(@id, 'quickSearchString')]"));
		searchBox.sendKeys("Berlin");
		WebElement searchButton = driver.findElement(By.xpath("//input[contains(@id, 'btnQuickSearchStart')]"));
		searchButton.click();
		
		String pageTitle = driver.findElement(By.tagName("h1")).getText();
		Assert.assertEquals(pageTitle, "Search Results");
		
		List<WebElement> results = driver.findElements(By.className("listItem"));
		Assert.assertNotEquals(results.size(), 0, "Results should be displayed.");
	}
	
	@Test(priority = 5)
	public void logout() {
		WebElement logoutButton = driver.findElement(By.xpath("//input[contains(@id, 'lnkLogout')]"));
		logoutButton.click();
		
		WebElement usernameBox = driver.findElement(By.xpath("//input[contains(@id, 'inputUsername')]"));
		Assert.assertTrue(usernameBox.isDisplayed(), "Logout was not successful.");
	}
	
	@AfterMethod
	public void failureScreenshot(ITestResult result) {
		if (result.getStatus() == ITestResult.FAILURE) {
			try {
				String screenshotPath = "./target/screenshot" + result.getName() + ".jpg";
				File screenshot = ((TakesScreenshot) driver).getScreenshotAs(org.openqa.selenium.OutputType.FILE);
				FileUtils.copyFile(screenshot, new File(screenshotPath));
			}
			catch (IOException exc) {}
		}
	}
	
	@AfterClass
	public void afterClass() {
		driver.quit();
	}
}
