# JasperRender

A HTTP component to render JasperReports from non-JVM applications.

After working in many different programming languages and frameworks, I have
yet to encounter a PDF-rendering library for reports that compares with the
functionality in JasperReports.
Although simple applications might be content with using HTML->PDF conversion
paths, these usually fall short once one wants to do things like intermediate
sums before a page break or other things specific to printed output.
Another feature I usually value is PDF1/A conformity as well as the
possibility of merging the report onto a letterhead.

## How It works

1. Clone the repository
2. Put your JRXML files along with any referenced files (images etc) into the `resources/reports` directory
3. For PDF/A compatibility: you'll require a proper sRGB ICC profile. You can find official profiles on [color.org](http://www.color.org/srgbprofiles.xalter).
 Download the ICC profile and put it into the `resources/icc` directory
4. Still work in progress: put any fonts used in your reports into the `resources/fonts` directory
5. Start the server with `./gradlew run`
6. You can now render reports by `POST`ing JSON files to `/render/<reportName>`

### WIP & planned features

- JRDataSource from JSON data (highest prio, obviously)
- Automatically generate font configuration XML and `jasperreports_extension.properties`
- Support merging reports onto pre-rendered letterhead PDFs
- PDF/A-3 compatibility & ZUGFeRD XML support