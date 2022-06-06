import path from "path";
import { uploadI18nFiles } from "./upload";
import { buildAndDownload, extractZip } from "./download";

// upload latest (english) source i18n files
uploadI18nFiles();

// download build target i18n files
buildAndDownload();

// extract to a temp dir
const temp_dir = path.join(__dirname, "../temp_dir");
extractZip("ankidroid.zip", temp_dir);