package live.karyl.anifetch.utils;

import com.google.gson.Gson;
import live.karyl.anifetch.models.AnimeSource;
import org.apache.catalina.util.URLEncoder;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class DashPlayerCreator {

    private static final String PROXY_URL = "https://proxy-cf.karyl.live/?id=";

    public String generateDashXML(AnimeSource animeSource) {
        try {
            int group = 1;
            int adaptationSet = 1;
            int representation = 0;
            var duration = Duration.ofMillis(animeSource.getVideoResources().get(0).getDuration());
            var durationString = "PT" + duration.toHours() + "H" + duration.toMinutesPart() + "M" + duration.toSecondsPart() + "S";

            XMLOutputFactory factory = XMLOutputFactory.newInstance();
            StringWriter writer = new StringWriter();
            XMLStreamWriter xmlWriter = factory.createXMLStreamWriter(writer);

            // Write the XML header
            xmlWriter.writeStartDocument();

            // Write the root element
            xmlWriter.writeStartElement("MPD");
            xmlWriter.writeAttribute("xmlns", "urn:mpeg:dash:schema:mpd:2011");
            xmlWriter.writeAttribute("profiles", "urn:mpeg:dash:profile:full:2011");
            xmlWriter.writeAttribute("minBufferTime", "PT1M");
            xmlWriter.writeAttribute("type", "static");
            xmlWriter.writeAttribute("mediaPresentationDuration", durationString);

            // Write the video adaptation set
            xmlWriter.writeStartElement("Period");
            xmlWriter.writeAttribute("start", "PT0S");
            xmlWriter.writeAttribute("duration", durationString);
            xmlWriter.writeStartElement("AdaptationSet");
            xmlWriter.writeAttribute("id", String.valueOf(adaptationSet));
            xmlWriter.writeAttribute("group", String.valueOf(group));
            xmlWriter.writeAttribute("segmentAlignment", "true");
            xmlWriter.writeAttribute("subsegmentStartsWithSAP", "1");
            xmlWriter.writeAttribute("maxWidth", "3840");
            xmlWriter.writeAttribute("maxHeight", "2160");
            xmlWriter.writeAttribute("maxFrameRate", "16000/656");
            xmlWriter.writeAttribute("startWithSAP", "1");
            xmlWriter.writeAttribute("par", "16:9");
            adaptationSet++;
            group++;

            // Write each video segment template
            for (var source : animeSource.getVideoResources()) {

                String videoUrl = source.getUrl();
                String quality = source.getQuality();

                if (quality.equals("4k")) {
                    videoUrl = source.getUrl();
                } else if (source.isUseHeader() && !source.getUrl().contains("-bstar1-")) {
                    String headers = new Gson().toJson(animeSource.getHeaders());
                    videoUrl = PROXY_URL + new URLEncoder().encode(source.getUrl() ,StandardCharsets.UTF_8)
                            +  "&header=" + headers + "&replace=true";
                }

                xmlWriter.writeStartElement("Representation");
                xmlWriter.writeAttribute("id", String.valueOf(representation));
                xmlWriter.writeAttribute("mimeType", source.getMimeType());
                xmlWriter.writeAttribute("sar", source.getSar());
                xmlWriter.writeAttribute("codecs", source.getCodecs());
                xmlWriter.writeAttribute("width", String.valueOf(source.getWidth()));
                xmlWriter.writeAttribute("height", String.valueOf(source.getHeight()));
                xmlWriter.writeAttribute("frameRate", source.getFrameRate());
                xmlWriter.writeAttribute("bandwidth", String.valueOf(source.getBandwidth()));
                xmlWriter.writeStartElement("BaseURL");
                xmlWriter.writeCharacters(videoUrl);
                xmlWriter.writeEndElement(); // BaseURL
                xmlWriter.writeStartElement("SegmentBase");
                xmlWriter.writeAttribute("indexRangeExact", "true");
                xmlWriter.writeAttribute("indexRange", source.getIndexRange());
                xmlWriter.writeStartElement("Initialization");
                xmlWriter.writeAttribute("range", source.getRange());
                xmlWriter.writeEndElement(); // Initialization
                xmlWriter.writeEndElement(); // SegmentBase
                xmlWriter.writeEndElement(); // Representation
                representation++;
            }

            xmlWriter.writeEndElement(); // AdaptationSet

            // Write the audio adaptation set
            xmlWriter.writeStartElement("AdaptationSet");
            xmlWriter.writeAttribute("id", String.valueOf(adaptationSet));
            xmlWriter.writeAttribute("group", String.valueOf(group));
            xmlWriter.writeAttribute("subsegmentAlignment", "true");
            xmlWriter.writeAttribute("subsegmentStartsWithSAP", "1");
            xmlWriter.writeAttribute("segmentAlignment", "true");
            xmlWriter.writeAttribute("startWithSAP", "1");
            adaptationSet++;
            group++;


            // Write each audio segment template
            for (var source : animeSource.getAudioResources()) {

                String headers = new Gson().toJson(animeSource.getHeaders());
                String audioUrl = PROXY_URL + new URLEncoder().encode(source.getUrl(), StandardCharsets.UTF_8)
                        +  "&header=" + headers + "&replace=true";

                xmlWriter.writeStartElement("Representation");
                xmlWriter.writeAttribute("id", String.valueOf(representation));
                xmlWriter.writeAttribute("mimeType", source.getMimeType());
                xmlWriter.writeAttribute("codecs", source.getCodecs());
                xmlWriter.writeAttribute("bandwidth", String.valueOf(source.getBandwidth()));
                xmlWriter.writeStartElement("BaseURL");
                xmlWriter.writeCharacters(audioUrl);
                xmlWriter.writeEndElement(); // BaseURL
                xmlWriter.writeStartElement("SegmentBase");
                xmlWriter.writeAttribute("indexRangeExact", "true");
                xmlWriter.writeAttribute("indexRange", source.getIndexRange());
                xmlWriter.writeStartElement("Initialization");
                xmlWriter.writeAttribute("range", source.getRange());
                xmlWriter.writeEndElement(); // Initialization
                xmlWriter.writeEndElement(); // SegmentBase
                xmlWriter.writeEndElement(); // Representation
                representation++;
            }

            xmlWriter.writeEndElement(); // AdaptationSet
            xmlWriter.writeEndElement(); // Period

            // Write the closing tags
            xmlWriter.writeEndElement(); // MPD
            xmlWriter.writeEndDocument();

            xmlWriter.flush();
            xmlWriter.close();

            return writer.toString();

        } catch (XMLStreamException e) {
            e.printStackTrace();
            return null;
        }
    }
}
